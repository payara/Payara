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

package fish.payara.nucleus.microprofile.config.source;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.config.support.TranslatedConfigView;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Config source for the microprofile-config-alias.properties file. This config source runs properties through the
 * TranslatedConfigView class to perform substitutions on aliases, system properties, and environment variables.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class AliasPropertiesConfigSource extends PayaraConfigSource implements ConfigSource {

    private final Properties properties;

    public AliasPropertiesConfigSource(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(configService.getMPConfig().getAliasPropertiesOrdinality());
    }

    @Override
    public Map<String, String> getProperties() {
        HashMap<String,String> result = new HashMap<>(properties.size());

        for (Object key : properties.keySet()) {
            String alias = properties.getProperty((String) key);
            String value = getValue((String) key);
            result.put(alias, value);
        }

        return result;
    }

    @Override
    public String getValue(String property) {
        String alias = properties.getProperty(property);

        // Null check for alias done in TranslatedConfigView.expandValue(alias)
        String value = TranslatedConfigView.expandValue(alias);

        // If returned value is null, or is the same as the pre-expanded alias, this means no match was found
        if (value == null || value.equals(alias)) {
            return null;
        }

        return value;
    }

    @Override
    public String getName() {
        return "Alias Properties";
    }

    AliasPropertiesConfigSource(boolean test, Properties properties) {
        super(test);
        this.properties = properties;
    }
}
