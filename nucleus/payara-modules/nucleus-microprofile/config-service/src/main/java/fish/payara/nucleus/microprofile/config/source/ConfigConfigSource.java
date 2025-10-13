/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class ConfigConfigSource extends PayaraConfigSource {

    private final String configurationName;

    public ConfigConfigSource(String configurationName) {
        this.configurationName = configurationName;
    }

    @Override
    public Map<String, String> getProperties() {
        Config config = domainConfiguration.getConfigNamed(configurationName);
        HashMap<String, String> result = new HashMap<>();
        if (config != null) {
            List<Property> properties = config.getProperty();
            for (Property property : properties) {
                if (property.getName().startsWith(PROPERTY_PREFIX)) {
                    result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public int getOrdinal() {
        String storedOrdinal = getValue("config_ordinal");
        if (storedOrdinal != null) {
            return Integer.parseInt(storedOrdinal);
        }
        return Integer.parseInt(configService.getMPConfig().getConfigOrdinality());
    }

    @Override
    public String getValue(String propertyName) {
        String result = null;
        Config config = domainConfiguration.getConfigs().getConfigByName(configurationName);
        if (config != null) {
            result = config.getPropertyValue(PROPERTY_PREFIX + propertyName);
        }
        return result;
    }

    @Override
    public String getName() {
        return "ServerConfig";
    }

    public boolean setValue(final String propertyName, final String propertyValue) throws TransactionFailure {
        boolean success = false;
        Config config = domainConfiguration.getConfigs().getConfigByName(configurationName);
        if (config != null) {
            Property p = config.getProperty(PROPERTY_PREFIX + propertyName);
            if (p == null) {
                ConfigSupport.apply(new SingleConfigCode<Config>() {
                    @Override
                    public Object run(Config config) throws TransactionFailure, PropertyVetoException {
                        Property prop = config.createChild(Property.class);
                        prop.setName(PROPERTY_PREFIX + propertyName);
                        prop.setValue(propertyValue);
                        config.getProperty().add(prop);
                        return null;
                    }
                }, config);
            } else {
                ConfigSupport.apply(new SingleConfigCode<Property>() {
                    @Override
                    public Object run(Property config) throws TransactionFailure, PropertyVetoException {
                        config.setValue(propertyValue);
                        return null;
                    }
                }, p);

            }
            success = true;
        }
        return success;
    }

    public boolean deleteValue(String propertyName) throws TransactionFailure {
        boolean result = false;
        Config config = domainConfiguration.getConfigs().getConfigByName(configurationName);
        if (config != null) {
            for (Property object : config.getProperty()) {
                if ((PROPERTY_PREFIX + propertyName).equals(object.getName())) {
                    ConfigSupport.deleteChild((ConfigBean) ConfigBean.unwrap(config), (ConfigBean) ConfigBean.unwrap(object));
                    result = true;
                }
            }
        }
        return result;
    }

}
