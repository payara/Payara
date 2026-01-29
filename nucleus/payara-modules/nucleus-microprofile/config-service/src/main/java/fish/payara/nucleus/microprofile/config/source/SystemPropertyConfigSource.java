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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class SystemPropertyConfigSource extends PayaraConfigSource {

    // Provides access to information on the server including;
    // command line, initial context, service locator, installation
    // Classloaders, config root for the server
    private ServerContext context;

    public SystemPropertyConfigSource() {
        context = Globals.getDefaultHabitat().getService(ServerContext.class);
    }

    /**
     * Only use in unit tests
     * @param test
     */
    SystemPropertyConfigSource(boolean test) {
        super(test);
    }



    @Override
    public Map<String, String> getProperties() {
        Properties props = System.getProperties();
        HashMap<String, String> result = new HashMap<>(props.size());
        for (String propertyName : props.stringPropertyNames()) {
            result.put(propertyName, props.getProperty(propertyName));
        }
        return result;
    }

    @Override
    public int getOrdinal() {
        String storedOrdinal = getValue("config_ordinal");
        if (storedOrdinal != null) {
            return Integer.parseInt(storedOrdinal);
        }
        return 400;
    }

    @Override
    public String getValue(String propertyName) {
        String result;
        result = System.getProperty(propertyName);
        if (result == null && context != null) {
            result = context.getConfigBean().getSystemPropertyValue(propertyName);
            if (result == null) {
                result = domainConfiguration.getSystemPropertyValue(propertyName);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "SystemProperty";
    }

}