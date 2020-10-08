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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.jdbc.config.JdbcResource;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author SusanRai
 */
public class DatasourceConfigSource extends PayaraConfigSource implements ConfigSource {

    private static final String DATASOURCE_CONFIG_PROPERTY_PREFIX = PROPERTY_PREFIX + "database.";
    private DatasourceConfigHelper datasourceConfigHelper = null;

    public DatasourceConfigSource(String datasourceJNDIname) {
        this.datasourceConfigHelper = new DatasourceConfigHelper(getJDBCResourceProperties(datasourceJNDIname));
    }

    @Override
    public Map<String, String> getProperties() {
        return datasourceConfigHelper.getAllConfigValues();
    }

    @Override
    public int getOrdinal() {
        return 400;
    }

    @Override
    public String getValue(String propertyName) {
        return datasourceConfigHelper.getConfigValue(propertyName);
    }

    @Override
    public String getName() {
        return "Datasource";
    }

    private Map<String, String> getJDBCResourceProperties(String datasourceJNDIname) {
        HashMap<String, String> result = new HashMap<>();

        if (datasourceJNDIname == null) {
            Iterator<JdbcResource> jdbcResources = domainConfiguration.getResources().getResources(JdbcResource.class).iterator();
            while (jdbcResources.hasNext()) {
                JdbcResource resource = jdbcResources.next();
                putJDBCResourceProperties(result, resource.getProperty());
            }
            return result;
        }

        JdbcResource jdbcResource = (JdbcResource) domainConfiguration.getResources().getResourceByName(JdbcResource.class, datasourceJNDIname);
        putJDBCResourceProperties(result, jdbcResource.getProperty());
        return result;
    }

    private void putJDBCResourceProperties(Map<String, String> result, List<Property> properties) {
        for (Property property : properties) {
            if (property.getName().startsWith(DATASOURCE_CONFIG_PROPERTY_PREFIX)) {
                result.put(property.getName().substring((DATASOURCE_CONFIG_PROPERTY_PREFIX).length()), property.getValue());
            }
        }
    }
}
