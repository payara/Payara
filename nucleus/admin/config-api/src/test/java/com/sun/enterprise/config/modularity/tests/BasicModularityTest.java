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
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;

import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.tests.utils.Utils;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;

import java.util.List;

import static org.junit.Assert.*;

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
    }


    @Test
    public void serializeConfigBean() {
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        ConfigBeanProxy prox = (ConfigBeanProxy) config.getExtensionByType(ConfigExtensionZero.class);
        String content = ConfigModularityUtils.serializeConfigBean(prox);
        assertEquals("Cannot serialize config beans properly", "<config-extension-zero dummy=\"dummy-value\"></config-extension-zero>", content);

    }

    @Test
    public void serializeConfigBeanByType() {
        String content = ConfigModularityUtils.serializeConfigBeanByType(ConfigExtensionOne.class, habitat);
        assertEquals("Cannot serialize config beans from type", "<config-extension-one custom-token=\"${CUSTOM_TOKEN}\">\n" +
                "  <property name=\"prop.foo\" value=\"prop.foo.value.custom\"></property>\n" +
                "</config-extension-one>", content);
    }


    @Test
    public void testConfigExtensionPatternImpl() {
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        SimpleConfigExtension simpleConfigExtension = config.getExtensionByType(SimpleConfigExtension.class);
        SimpleExtensionTypeTwo typeTwo = simpleConfigExtension.getExtensionByType(SimpleExtensionTypeTwo.class);
        assertNotNull("cannot get extension using extensionmethod", typeTwo);
        assertEquals("Retrieved extension is not from the right type... ", "attribute.two", typeTwo.getAttributeTwo());
    }

    @Test
    public void testLoadingAndApplyingModuleConfigurationFile() {
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        ConfigExtensionTwo ext = config.getExtensionByType(ConfigExtensionTwo.class);
        assertEquals("The system property is overridden while it should have not", "user.customized.value", config.getSystemProperty("CUSTOM_TOKEN").getValue());
        assertTrue("Failed to add the extension from module configuration file: ", config.checkIfExtensionExists(ConfigExtensionTwo.class));
    }


    @Test
    public void testHasNoCustomization() {
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        assertNull("The @HasNocustomization annotation is broken", config.getExtensionByType(ConfigExtensionThree.class));
    }

    @Test
    public void getCurrentConfigurationForConfigBean() throws Exception {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true);
        com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue def = ConfigModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, true).get(0);
        SimpleExtensionTypeOne simple = ConfigModularityUtils.getCurrentConfigBeanForDefaultValue(def, habitat);
        assertNotNull("Cannot get config bean of a module based on the default module configuration information", simple);
    }


    @Test
    public void testOverrideOnStartup() {
        ensureRunLevel(2);
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        SystemProperty property = config.getSystemProperty("startup.overriding.property");
        assertEquals("The system property is not overridden during startup, Something wrong with processing @ActivateOnStartup", "new-custom-value", property.getValue());
    }

    //TODO add more tests to cover token processing and i18n support

    private void ensureRunLevel(int runlevel) {
        RunLevelController controller = habitat.getService(RunLevelController.class);
        controller.proceedTo(runlevel);
    }

}
