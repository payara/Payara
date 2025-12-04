/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class EnvironmentConfigSource implements ConfigSource {

    @Override
    public Map<String, String> getProperties() {
        return getEnv();
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public int getOrdinal() {
        String ordinalVal = getEnv().getOrDefault("config_ordinal", "300");
        return Integer.parseInt(ordinalVal);
    }

    @Override
    public String getValue(String propertyName) {

        // search environment variables as defined in the spec
        // https://github.com/eclipse/microprofile-config/blob/main/spec/src/main/asciidoc/configsources.asciidoc

        //Done this way to resolve PAYARA-3064 instead of genenv(propertyname)
        //as windows is case-insensitive but Java is not
        String result = getEnv(propertyName);

        if (result == null) {
            // replace all non-alphanumeric characters
            propertyName = propertyName.replaceAll("[^A-Za-z0-9]", "_");
            result = getEnv(propertyName);
        }

        if (result == null) {
            propertyName = propertyName.toUpperCase();
        }
        return getEnv(propertyName);
    }

    @Override
    public String getName() {
        return "Environment";
    }

    private static Map<String, String> getEnv() {
        final PrivilegedAction<Map<String, String>> action = System::getenv;
        return AccessController.doPrivileged(action);
    }

    private static String getEnv(final String propertyName) {
        return getEnv().get(propertyName);
    }
}