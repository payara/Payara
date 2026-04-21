/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.JavaConfig;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.exousia.modules.def.DefaultPolicy;
import org.glassfish.exousia.modules.def.DefaultPolicyConfigurationFactory;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup service that ensures JACC provider configuration compatibility with Payara 6+.
 *
 * On startup, this service inspects all configurations in {@code domain.xml} and performs
 * the following migrations where necessary:
 *
 *   - Replaces legacy policy provider {@value #OLD_POLICY_PROVIDER} with {@link DefaultPolicy}.
 *   - Replaces legacy policy configuration factory {@value #OLD_POLICY_CONFIGURATION_FACTORY}
 *     with {@link DefaultPolicyConfigurationFactory}.
 *   - Ensures the {@code jakarta.security.jacc.PolicyFactory.provider} JVM option is set.
 *     If a custom value is already present, it is left unchanged.
 *
 * @author luisneto
 * @author kalinchan
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class JaccProviderCompatibilityStartup implements PostConstruct {

    private static final Logger logger = Logger.getLogger(JaccProviderCompatibilityStartup.class.getName());

    public static final String OLD_POLICY_PROVIDER =
            "fish.payara.security.jacc.provider.PolicyProviderImpl";
    public static final String OLD_POLICY_CONFIGURATION_FACTORY =
            "fish.payara.security.jacc.provider.PolicyConfigurationFactoryImpl";

    private static final String NEW_POLICY_PROVIDER =
            DefaultPolicy.class.getCanonicalName();
    private static final String NEW_POLICY_CONFIGURATION_FACTORY =
            DefaultPolicyConfigurationFactory.class.getCanonicalName();

    private static final String JACC_POLICY_FACTORY_JVM_OPTION_KEY =
            "-Djakarta.security.jacc.PolicyFactory.provider=";

    private static final String JACC_POLICY_FACTORY_JVM_OPTION =
            JACC_POLICY_FACTORY_JVM_OPTION_KEY + "org.glassfish.exousia.modules.def.DefaultPolicyFactory";

    @Inject
    private Configs configs;

    @Override
    public void postConstruct() {
        logger.fine("Starting JACC provider compatibility migration");

        for (Config config : configs.getConfig()) {
            migrateJaccProviders(config);
            ensureJaccPolicyFactoryJvmOption(config);
        }

        logger.fine("JACC provider compatibility migration complete");
    }

    private void migrateJaccProviders(Config config) {
        List<JaccProvider> jaccProviders = config.getSecurityService().getJaccProvider();

        for (JaccProvider jaccProvider : jaccProviders) {
            String currentPolicyProvider = jaccProvider.getPolicyProvider();
            String currentPolicyConfigFactory = jaccProvider.getPolicyConfigurationFactoryProvider();

            String updatedPolicyProvider = migrateClassName(
                    currentPolicyProvider, OLD_POLICY_PROVIDER, NEW_POLICY_PROVIDER,
                    "policy provider");
            String updatedPolicyConfigFactory = migrateClassName(
                    currentPolicyConfigFactory, OLD_POLICY_CONFIGURATION_FACTORY, NEW_POLICY_CONFIGURATION_FACTORY,
                    "policy configuration factory");

            boolean policyProviderChanged = !updatedPolicyProvider.equals(currentPolicyProvider);
            boolean policyConfigFactoryChanged = !updatedPolicyConfigFactory.equals(currentPolicyConfigFactory);

            if (policyProviderChanged || policyConfigFactoryChanged) {
                updateJaccProvider(jaccProvider, updatedPolicyProvider, updatedPolicyConfigFactory);
            }
        }
    }

    /**
     * Returns the new class name if the current one matches the old one; otherwise returns the current one unchanged.
     */
    private String migrateClassName(String current, String oldName, String newName, String description) {
        if (oldName.equals(current)) {
            logger.log(Level.INFO, "Migrating JACC {0}: [{1}] -> [{2}]",
                    new Object[]{description, oldName, newName});
            return newName;
        }
        return current;
    }

    private void updateJaccProvider(JaccProvider jaccProvider,
                                    String newPolicyProvider,
                                    String newPolicyConfigFactory) {
        try {
            ConfigSupport.apply(param -> {
                param.setPolicyProvider(newPolicyProvider);
                param.setPolicyConfigurationFactoryProvider(newPolicyConfigFactory);
                return param;
            }, jaccProvider);

            logger.log(Level.INFO, "Successfully updated JACC provider configuration");
        } catch (TransactionFailure e) {
            logger.log(Level.SEVERE, "Failed to update JACC provider configuration", e);
            throw new RuntimeException("Failed to update JACC provider configuration", e);
        }
    }

    private void ensureJaccPolicyFactoryJvmOption(Config config) {
        JavaConfig javaConfig = config.getJavaConfig();
        List<String> jvmOptions = javaConfig.getJvmOptions();

        Optional<String> existing = jvmOptions.stream()
                .filter(opt -> opt.startsWith(JACC_POLICY_FACTORY_JVM_OPTION_KEY))
                .findFirst();

        if (existing.isPresent()) {
            if (existing.get().equals(JACC_POLICY_FACTORY_JVM_OPTION)) {
                logger.fine("JACC PolicyFactory JVM option already present with correct value, skipping");
            } else {
                logger.log(Level.FINE,
                        "JACC PolicyFactory JVM option is set to a custom value [{0}], skipping override",
                        existing.get());
            }
            return;
        }

        logger.log(Level.INFO, "Adding missing JACC PolicyFactory JVM option: {0}", JACC_POLICY_FACTORY_JVM_OPTION);
        jvmOptions.add(JACC_POLICY_FACTORY_JVM_OPTION);

        try {
            ConfigSupport.apply(jconfig -> {
                jconfig.setJvmOptions(jvmOptions);
                return config;
            }, javaConfig);

            logger.log(Level.INFO, "Successfully added JACC PolicyFactory JVM option");
        } catch (TransactionFailure e) {
            logger.log(Level.SEVERE, "Failed to add JACC PolicyFactory JVM option", e);
            throw new RuntimeException("Failed to add JACC PolicyFactory JVM option", e);
        }
    }
}