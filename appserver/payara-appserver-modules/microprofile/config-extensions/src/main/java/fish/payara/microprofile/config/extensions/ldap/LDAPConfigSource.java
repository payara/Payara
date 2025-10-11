/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.ldap;

import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.ConfigProviderResolverImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Gaurav Gupta
 */
@Service(name = "ldap-config-source")
public class LDAPConfigSource extends ConfiguredExtensionConfigSource<LDAPConfigSourceConfiguration> {
    
    private static final Logger LOGGER = Logger.getLogger(LDAPConfigSource.class.getName());

    private LDAPConfigSourceHelper ldapConfigSourceHelper;

    @Override
    public void bootstrap() {
        this.ldapConfigSourceHelper = new LDAPConfigSourceHelper(configuration);
    }

    @Override
    public void destroy() {
        this.ldapConfigSourceHelper = null;
    }
    
    @Override
    public Map<String, String> getProperties() {
        if (ldapConfigSourceHelper == null) {
            printMisconfigurationMessage();
            return new HashMap<>();
        }
        return ldapConfigSourceHelper.getAllConfigValues();
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        if (ldapConfigSourceHelper == null) {
            printMisconfigurationMessage();
            return null;
        }
        return ldapConfigSourceHelper.getConfigValue(propertyName);
    }

    @Override
    public boolean deleteValue(String value) {
        return false;
    }

    @Override
    public boolean setValue(String key, String value) {
        return false;
    }

    @Override
    public String getSource() {
        return "ldap";
    }

    @Override
    public String getName() {
        return "ldap";
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(
                Globals.getDefaultHabitat()
                        .getService(ConfigProviderResolverImpl.class)
                        .getMPConfig().getLdapOrdinality()
        );
    }

    private static void printMisconfigurationMessage() {
        LOGGER.warning("LDAP Config Source isn't configured correctly.");
    }



}
