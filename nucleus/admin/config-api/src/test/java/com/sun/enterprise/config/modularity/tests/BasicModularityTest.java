/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.config.modularity.tests;

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;

import org.glassfish.tests.utils.Utils;

import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing Basic Functionality of Config Modularity API
 *
 * @author Masoud Kalali
 */
public class BasicModularityTest extends ConfigApiTest {

    Habitat habitat = Utils.instance.getHabitat(this);

    @Override
    public String getFileName() {
        return "Config-Modularity";
    }

    @Test
    public void locationTest() {
        String location = "domain/configs/config[$CURRENT_INSTANCE_CONFIG_NAME]/config-extension-one/property[prop.foo]";
        Class owningClass = ConfigModularityUtils.getOwningClassForLocation(location, habitat);
        assertNotNull("Cannot find owning class for: " + location, owningClass);
        assertEquals("Cannot find the right owning class for location", Property.class.getName(), owningClass.getName());
    }

    @Test
    public void owningObjectTest() {
        String location = "domain/configs/config[$CURRENT_INSTANCE_CONFIG_NAME]/config-extension-one/property[prop.foo]";
        ConfigBeanProxy obj = ConfigModularityUtils.getOwningObject(location, habitat);
        assertNotNull("Cannot find owning object for: " + location, obj);
        assertEquals("Getting Owning object for location is not right", "prop.foo.value.custom", ((Property) obj).getValue());
    }

    @Test
    public void moduleConfigurationXmlParserTest() {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true);
        assertEquals("Incorrect number of config bean configuration read ", 2, values.size());
        ConfigCustomizationToken token = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true).get(0).getCustomizationTokens().get(0);
        assertEquals("Customization Token reading broken ", "CUSTOM_TOKEN", token.getKey());
        assertEquals("Customization Token reading broken ", "token-default-value", token.getDefaultValue());
//TODO Also check for the localization
//        assertEquals("Customization Token reading broken ", "title", token.getTitle());
//        assertEquals("Customization Token reading broken ", "token description", token.getDescription());

    }


    @Test
    public void serializeConfigBean() {
        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        ConfigBeanProxy prox = config.getExtensionByType(ConfigExtensionZero.class);
        String content = ConfigModularityUtils.serializeConfigBean(prox);
        assertEquals("Cannot serialize config beans properly", "<config-extension-zero dummy=\"dummy-value\"></config-extension-zero>", content);

    }

    @Test
    public void serializeConfigBeanByType() {
        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        String content = ConfigModularityUtils.serializeConfigBeanByType(ConfigExtensionOne.class, habitat);
        assertEquals("Cannot serialize config beans from type", "<config-extension-one custom-token=\"${CUSTOM_TOKEN}\">\n" +
                "  <property name=\"prop.foo\" value=\"prop.foo.value.custom\"></property>\n" +
                "</config-extension-one>", content);
    }

    @Test
    @Ignore
    public void testGetExtensionByType() {
        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        ConfigExtensionTwo ext = (ConfigExtensionTwo)
                config.getExtensionByType(ConfigExtensionTwo.class);
        assertEquals("The system property is overriden while it should have not", "token-default-value", ext.getAttributeTwo());

    }


    @Test
    @Ignore //Ignored but left here to investigate a possible bug
    public void testLoadingAndApplyingModuleConfigurationFile() {
        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        ConfigExtensionOne ext = (ConfigExtensionOne)
                config.getExtensionByType(ConfigExtensionOne.class);
        assertEquals("The system property is overriden while it should have not", "user.customized", ext.getCustomToken());

    }

    @Test
    @Ignore
    public void fromXmlToConfigBeanTest() throws Exception {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true);
        com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue def = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true).get(0);
//        SimpleExtensionTypeOne c = habitat.getService(SimpleExtensionTypeOne.class);
//                System.out.println("So the config bean is around because we can get it and print it's property");
//        System.out.println(c.getAttributeOne());
//                System.out.println("But the method com.sun.enterprise.config.modularity.ConfigModularityUtils.getClassForFullName(ConfigModularityUtils.java:767) Goes NPE");
//        ConfigModularityUtils.getClassForFullName(SimpleExtensionTypeOne.class.getName(), habitat);
        SimpleExtensionTypeOne simple = ConfigModularityUtils.getCurrentConfigBeanForDefaultValue(def, habitat);
        assertNotNull("Cannot get config bean of a module based on the default module configuration information", simple);
        assertEquals("The retrieved current configuration of the module is not as is in domain.xml", "token-default-value", simple.getAttributeOne());
    }

    @Test
    @Ignore
    public void fromClassNameToClassTest() throws Exception {

        // This part passes as the configuration for the class is present in the domain.xml
        Class clz = ConfigModularityUtils.getClassForFullName(ConfigExtensionZero.class.getName(), habitat);
        assertNotNull("Cannot get config bean class using the class name", clz);
        assertEquals("The mapped class is not the same as the provided class name", ConfigExtensionZero.class.getName(), clz.getName());

        // this part fails as the configuration is not present in domain.xml which was is a regression somewhere
         clz = ConfigModularityUtils.getClassForFullName(ConfigExtensionTwo.class.getName(), habitat);
        assertNotNull("Cannot get config bean class using the class name", clz);
        assertEquals("The mapped class is not the same as the provided class name", ConfigExtensionTwo.class.getName(), clz.getName());
    }


}
