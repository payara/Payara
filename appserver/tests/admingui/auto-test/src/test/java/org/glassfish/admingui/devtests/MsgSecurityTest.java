/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.devtests;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertEquals;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class MsgSecurityTest extends BaseSeleniumTestClass {

    @Test
    public void testCreateMsgSecurityConfigWithNoDefaultProvider() {
        gotoDasPage();
        final String providerName = "provider" + generateRandomString();
        final String configName= "Config-" + generateRandomString();
        final String propertyName= "property-" + generateRandomString();
        final String layer = "HttpServlet";

        copyConfig("default-config", configName);
        gotoDasPage();
        createMsgSecurityConfig(configName, layer, providerName, "client", false ,propertyName);

        String prefix = getTableRowByValue("propertyForm:configs", layer, "col1");
        assertEquals(layer, getText(prefix + "col1:authlayer"));
        //since we didn't mark this as default provider, ensure it is not listed in the table.
        assertEquals("", getText(prefix + "col2"));
        assertEquals(providerName, getText(prefix + "col3"));
        clickAndWait(prefix + "col1:authlayer" );

        //clean up by removing the config.
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }

    @Test
    public void testCreateMsgSecurityConfigWithDefaultProvider() {
        gotoDasPage();
        final String providerName = "provider" + generateRandomString();
        final String configName= "Config-" + generateRandomString();
        final String propertyName= "property-" + generateRandomString();
        final String layer = "HttpServlet";

        copyConfig("default-config", configName);
        gotoDasPage();
        createMsgSecurityConfig(configName, layer, providerName, "server", true, propertyName);

        String prefix = getTableRowByValue("propertyForm:configs", layer, "col1");
        assertEquals(layer, getText(prefix + "col1:authlayer"));
        //since we didn't mark this as default provider, ensure it is not listed in the table.
        assertEquals(providerName, getText(prefix + "col2:defaultprov"));
        assertEquals("", getText(prefix + "col3"));
        clickAndWait(prefix + "col1:authlayer" );
        assertEquals(configName, getText("propertyForm:propertySheet:configNameSheet:configName:configName"));

        //clean up by removing the config.
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }

    @Test
    public void testCreateMsgSecurityConfigWithDefaultClientProvider() {
        gotoDasPage();
        final String providerName = "provider" + generateRandomString();
        final String configName= "Config-" + generateRandomString();
        final String propertyName= "property-" + generateRandomString();
        final String layer = "HttpServlet";

        copyConfig("default-config", configName);
        gotoDasPage();
        createMsgSecurityConfig(configName, layer, providerName, "client", true, propertyName);

        String prefix = getTableRowByValue("propertyForm:configs", layer, "col1");
        assertEquals(layer, getText(prefix + "col1:authlayer"));
        //since we didn't mark this as default provider, ensure it is not listed in the table.
        assertEquals("", getText(prefix + "col2"));
        assertEquals(providerName, getText(prefix + "col3:defaultclientprov"));
        clickAndWait(prefix + "col1:authlayer" );
        assertEquals(configName, getText("propertyForm:propertySheet:configNameSheet:configName:configName"));

        //clean up by removing the config.
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }

    @Test
    public void testCreateMsgSecurityConfigWithDefaultClientServerProvider() {
        gotoDasPage();
        final String providerName = "provider" + generateRandomString();
        final String configName= "Config-" + generateRandomString();
        final String propertyName= "property-" + generateRandomString();
        final String layer = "HttpServlet";

        copyConfig("default-config", configName);
        gotoDasPage();
        createMsgSecurityConfig(configName, layer, providerName, "client-server", true, propertyName);

        String prefix = getTableRowByValue("propertyForm:configs", layer, "col1");
        assertEquals(layer, getText(prefix + "col1:authlayer"));
        //since we didn't mark this as default provider, ensure it is not listed in the table.
        assertEquals(providerName, getText(prefix + "col2:defaultprov"));
        assertEquals(providerName, getText(prefix + "col3:defaultclientprov"));
        clickAndWait(prefix + "col1:authlayer" );
        assertEquals(configName, getText("propertyForm:propertySheet:configNameSheet:configName:configName"));
        //clean up by removing the config.
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }
    

    @Test
    public void testCreateAdditionalProviders() {
        gotoDasPage();
        final String providerName = "provider" + generateRandomString();
        final String providerName2 = "provider" + generateRandomString();
        final String configName= "Config-" + generateRandomString();
        final String propertyName= "property-" + generateRandomString();
        final String layer = "HttpServlet";

        copyConfig("default-config", configName);
        gotoDasPage();
        createMsgSecurityConfig(configName, layer, providerName, "client", true, propertyName);


        String prefix = getTableRowByValue("propertyForm:configs", layer, "col1");
        assertEquals(layer, getText(prefix + "col1:authlayer"));
        //since we didn't mark this as default provider, ensure it is not listed in the table.
        assertEquals("", getText(prefix + "col2"));
        assertEquals(providerName, getText(prefix + "col3:defaultclientprov"));
        clickAndWait("propertyForm:configs:rowGroup1:0:col1:authlayer" );
        assertEquals(configName, getText("propertyForm:propertySheet:configNameSheet:configName:configName"));

        clickAndWait("propertyForm:msgSecurityTabs:providers" );
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");

        setFieldValue("propertyForm:propertySheet:providerConfSection:ProviderIdTextProp:ProviderIdText", providerName2);
        if (!driver.findElement(By.id("propertyForm:propertySheet:providerConfSection:DefaultProviderProp:def")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:providerConfSection:DefaultProviderProp:def");
        }
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:providerConfSection:ProviderTypeProp:ProviderType")));
        select.selectByVisibleText("server");
        setFieldValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName", "CLASSNAME");
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:requestPolicySection:AuthSourceProp:AuthSource")));
        select1.selectByVisibleText("sender");
        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertySheet:requestPolicySection:AuthRecipientProp:AuthRecipient")));
        select2.selectByVisibleText("before-content");
        Select select3 = new Select(driver.findElement(By.id("propertyForm:propertySheet:responsePolicySection:AuthSourceProp:AuthSource")));
        select3.selectByVisibleText("content");
        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", propertyName);
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "DESC");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");

        String proPrefix = getTableRowByValue("propertyForm:configs", providerName2, "col1");
        assertEquals("server", getText(proPrefix + "col2:provType"));
        assertEquals("true", getText(proPrefix + "col3:default"));
        assertEquals("CLASSNAME", getText(proPrefix + "col4:defaultclientprov"));

        clickAndWait(proPrefix+"col1:authlayer" );
        assertEquals(configName, getText("propertyForm:propertySheet:configNameSheet:configName:configName"));

        Select select4 = new Select(driver.findElement(By.id("propertyForm:propertySheet:requestPolicySection:AuthSourceProp:AuthSource")));
        Select select5 = new Select(driver.findElement(By.id("propertyForm:propertySheet:requestPolicySection:AuthRecipientProp:AuthRecipient")));
        Select select6 = new Select(driver.findElement(By.id("propertyForm:propertySheet:responsePolicySection:AuthSourceProp:AuthSource")));
        Select select7 = new Select(driver.findElement(By.id("propertyForm:propertySheet:responsePolicySection:AuthRecipientProp:AuthRecipient")));
        assertEquals(select4.getFirstSelectedOption().getAttribute("value"), "sender");
        assertEquals(select5.getFirstSelectedOption().getAttribute("value"), "before-content");
        assertEquals(select6.getFirstSelectedOption().getAttribute("value"), "content");
        assertEquals(select7.getFirstSelectedOption().getAttribute("value"), "");
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //clean up by removing the config.
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }


    public void createMsgSecurityConfig(String configName, String layer, String providerName, String type, boolean isDefault, String propertyName){

        clickAndWait("treeForm:tree:configurations:" + configName + ":security:messageSecurity:messageSecurity_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        isElementPresent("propertyForm:propertySheet:propertySheetSection:AuthLayerProp:AuthLayer");
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:AuthLayerProp:AuthLayer")));
        select.selectByVisibleText(layer);
        setFieldValue("propertyForm:propertySheet:providerConfSection:ProviderIdTextProp:ProviderIdText", providerName);
        if (!driver.findElement(By.id("propertyForm:propertySheet:providerConfSection:DefaultProviderProp:def")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:providerConfSection:DefaultProviderProp:def");
        }
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:providerConfSection:ProviderTypeProp:ProviderType")));
        select1.selectByVisibleText(type);
        setFieldValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName", "CLASSNAME");
        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", propertyName);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "DESC");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
    }

    public void copyConfig(String srcName, String newConfigName) {
        clickAndWait("treeForm:tree:configurations:configurations_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameProp:Name", newConfigName);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:ConfigProp:Config")));
        select.selectByVisibleText(srcName);
        clickAndWait("propertyForm:propertyContentPage:topButtons:okButton");
        
        String prefix = getTableRowByValue("propertyForm:configs", newConfigName, "col1");
        assertEquals(newConfigName, getText(prefix + "col1:link"));
    }
}
