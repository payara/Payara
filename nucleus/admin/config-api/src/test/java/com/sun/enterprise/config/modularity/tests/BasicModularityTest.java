/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.modularity.customization.FileTypeDetails;
import com.sun.enterprise.config.modularity.customization.PortTypeDetails;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.tests.utils.Utils;
import org.junit.Test;
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

    private ServiceLocator habitat = Utils.instance.getHabitat(this);

    ConfigModularityUtils configModularityUtils = habitat.getService(ConfigModularityUtils.class);

    @Override
    public String getFileName() {
        return "Config-Modularity";
    }

    @Test
    public void fromClassNameToClassTest() throws Exception {

        // This part passes as the configuration for the class is present in the domain.xml
        Class clz = configModularityUtils.getClassForFullName(ConfigExtensionZero.class.getName());
        assertNotNull("Cannot get config bean class using the class name", clz);
        assertEquals("The mapped class is not the same as the provided class name", ConfigExtensionZero.class.getName(), clz.getName());

        // this part fails as the configuration is not present in domain.xml which was is a regression somewhere
        clz = configModularityUtils.getClassForFullName(ConfigExtensionTwo.class.getName());
        assertNotNull("Cannot get config bean class using the class name", clz);
        assertEquals("The mapped class is not the same as the provided class name", ConfigExtensionTwo.class.getName(), clz.getName());
    }

    @Test
    public void locationTest() {
        String location = "domain/configs/config[$CURRENT_INSTANCE_CONFIG_NAME]/config-extension-one/property[prop.foo]";
        Class owningClass = configModularityUtils.getOwningClassForLocation(location);
        assertNotNull("Cannot find owning class for: " + location, owningClass);
        assertEquals("Cannot find the right owning class for location", Property.class.getName(), owningClass.getName());
    }

    @Test
    public void owningObjectTest() {
        String location = "domain/configs/config[$CURRENT_INSTANCE_CONFIG_NAME]/config-extension-one/property[prop.foo]";
        ConfigBeanProxy obj = configModularityUtils.getOwningObject(location);
        assertNotNull("Cannot find owning object for: " + location, obj);
        assertEquals("Getting Owning object for location is not right", "prop.foo.value.custom", ((Property) obj).getValue());
    }

    @Test
    public void moduleConfigurationXmlParserTest() {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = configModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, "admin-");
        assertEquals("Incorrect number of config bean configuration read ", 2, values.size());
        ConfigCustomizationToken token = configModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, "embedded-").get(0).getCustomizationTokens().get(0);
        assertEquals("Customization Token reading broken ", "CUSTOM_TOKEN", token.getName());
        assertEquals("Customization Token reading broken ", "token-default-value", token.getValue());
    }


    @Test
    public void serializeConfigBean() {
        Config config = habitat.<Config>getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
        ConfigBeanProxy prox = (ConfigBeanProxy) config.getExtensionByType(ConfigExtensionZero.class);
        String content = configModularityUtils.serializeConfigBean(prox);
        assertEquals("Cannot serialize config beans properly", "<config-extension-zero dummy=\"dummy-value\"></config-extension-zero>", content);

    }

    @Test
    public void serializeConfigBeanByType() {
        String content = configModularityUtils.serializeConfigBeanByType(ConfigExtensionOne.class);
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
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = configModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, "admin-");
        com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue def = configModularityUtils.getDefaultConfigurations(SimpleExtensionTypeOne.class, "embedded-").get(0);
        SimpleExtensionTypeOne simple = configModularityUtils.getCurrentConfigBeanForDefaultValue(def);
        assertNotNull("Cannot get config bean of a module based on the default module configuration information", simple);
    }


    @Test
    public void testLoadingAdminFile() throws Exception {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = configModularityUtils.getDefaultConfigurations(ConfigExtensionTwo.class, "admin");
        assertEquals("Incorrect customization type loaded ", ConfigCustomizationToken.CustomizationType.FILE, values.get(0).getCustomizationTokens().get(0).getCustomizationType());
        assertEquals("Incorrect customization details value ", FileTypeDetails.FileExistCondition.MUST_EXIST, ((FileTypeDetails) values.get(0).getCustomizationTokens().get(0).getTokenTypeDetails()).getExistCondition());
    }

    @Test
    public void testLoadingEmbeddedFile() throws Exception {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = values = configModularityUtils.getDefaultConfigurations(ConfigExtensionTwo.class, "embedded");
        assertEquals("Incorrect customization type loaded ", ConfigCustomizationToken.CustomizationType.PORT, values.get(0).getCustomizationTokens().get(0).getCustomizationType());
        assertEquals("Incorrect customization details value ", "1000", ((PortTypeDetails) values.get(0).getCustomizationTokens().get(0).getTokenTypeDetails()).getBaseOffset());
        assertEquals("validation expression is returned incorrectly ", "[a-zA-Z0-9]+", values.get(0).getCustomizationTokens().get(0).getValidationExpression());
    }

    @Test
    public void testLoadingDefaultFile() throws Exception {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = configModularityUtils.getDefaultConfigurations(ConfigExtensionTwo.class, "non-existing-runtime-type");
        assertEquals("validation expression is returned incorrectly ", ".*[0-9]{10}.*", values.get(0).getCustomizationTokens().get(0).getValidationExpression());
    }

    @Test
    public void tesOnTheFlyConfigurationGenerationMethod() {
        List<com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue> values = configModularityUtils.getDefaultConfigurations(SimpleExtensionThree.class, "non-existing-runtime-type");
        assertEquals("On the fly config generation/reading is broken", "<xml-doc></xml-doc>", values.get(0).getXmlConfiguration());
    }


    @Test
    public void testRanking() {
        Domain d = habitat.<Domain>getService(Domain.class);
        RankedConfigBean rankedConfigBean = d.getExtensionByType(RankedConfigBean.class);
        assertEquals("invalid current value",  "simple-value-zero",rankedConfigBean.getSimpleAttribute());
        ensureRunLevel(4);
         rankedConfigBean = d.getExtensionByType(RankedConfigBean.class);
        assertEquals("invalid current value", "simple-value-one", rankedConfigBean.getSimpleAttribute() );
    }

    //TODO add more tests to cover token processing and i18n support

    private void ensureRunLevel(int runlevel) {
        RunLevelController controller = habitat.getService(RunLevelController.class);
        controller.proceedTo(runlevel);
    }

}
