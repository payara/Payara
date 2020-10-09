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

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.jdbc.config.JdbcResource;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

public class JDBCConfigSource extends PayaraConfigSource implements ConfigSource {

    private static final String JDBC_CONFIG_PROPERTY_PREFIX = PROPERTY_PREFIX + "jdbc.";
    private final JDBCConfigSourceHelper jdbcConfigHelper;
    private final String jndiName;

    public JDBCConfigSource(String jndiName) {
        this.jndiName = jndiName;
        this.jdbcConfigHelper = new JDBCConfigSourceHelper(getJDBCConfigProperties());
    }

    @Override
    public Map<String, String> getProperties() {
        return jdbcConfigHelper.getAllConfigValues();
    }

    @Override
    public int getOrdinal() {
        return 400;
    }

    @Override
    public String getValue(String propertyName) {
        return jdbcConfigHelper.getConfigValue(propertyName);
    }

    @Override
    public String getName() {
        return "JDBC";
    }

    private Map<String, String> getJDBCConfigProperties() {
        HashMap<String, String> result = new HashMap<>();

        if (jndiName == null) {
            Iterator<JdbcResource> jdbcResources = domainConfiguration.getResources().getResources(JdbcResource.class).iterator();
            while (jdbcResources.hasNext()) {
                JdbcResource resource = jdbcResources.next();
                putJDBCConfigProperties(result, resource.getProperty());
            }
            return result;
        }

        JdbcResource resource = (JdbcResource) domainConfiguration.getResources().getResourceByName(JdbcResource.class, jndiName);
        if (resource != null) {
            putJDBCConfigProperties(result, resource.getProperty());
        }
        return result;
    }

    private void putJDBCConfigProperties(Map<String, String> result, List<Property> properties) {
        for (Property property : properties) {
            if (property.getName().startsWith(JDBC_CONFIG_PROPERTY_PREFIX)) {
                result.put(property.getName().substring((JDBC_CONFIG_PROPERTY_PREFIX).length()), property.getValue());
            }
        }
    }

    public boolean setValue(final String propertyName, final String propertyValue) throws TransactionFailure {
        boolean result = false;
        JdbcResource resource = (JdbcResource) domainConfiguration.getResources().getResourceByName(JdbcResource.class, jndiName);
        if (resource != null) {
            // does the property exist
            Property property = resource.getProperty(JDBC_CONFIG_PROPERTY_PREFIX + propertyName);
            if (property == null) {
                ConfigSupport.apply(new SingleConfigCode<JdbcResource>() {
                    @Override
                    public Object run(JdbcResource config) throws TransactionFailure, PropertyVetoException {
                        Property prop = config.createChild(Property.class);
                        prop.setName(JDBC_CONFIG_PROPERTY_PREFIX + propertyName);
                        prop.setValue(propertyValue);
                        config.getProperty().add(prop);
                        return null;
                    }
                }, resource);
            } else {
                ConfigSupport.apply(new SingleConfigCode<Property>() {
                    @Override
                    public Object run(Property config) throws TransactionFailure, PropertyVetoException {
                        config.setValue(propertyValue);
                        return null;
                    }
                }, property);

            }
            result = true;
        }
        return result;
    }

    public boolean deleteValue(String propertyName) throws TransactionFailure {
        boolean result = false;

        JdbcResource resource = (JdbcResource) domainConfiguration.getResources().getResourceByName(JdbcResource.class, jndiName);
        if (resource != null) {
            for (Property property : resource.getProperty()) {
                if ((JDBC_CONFIG_PROPERTY_PREFIX + propertyName).equals(property.getName())) {
                    ConfigSupport.deleteChild((ConfigBean) ConfigBean.unwrap(resource), (ConfigBean) ConfigBean.unwrap(property));
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
}
