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

import java.util.ArrayList;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class SecurityTest extends BaseSeleniumTestClass {
    ArrayList<String> list = new ArrayList(); {list.add("server-config"); list.add("new-config");}


//    @Test
    // TODO: The page has a component without an explicit ID. Disabling the test for now.
    public void testSecurityPage() {
        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":jvmSettings:jvmSettings_link");
            clickAndWait("propertyForm:javaConfigTab:jvmOptions");
            waitForElementPresent("TtlTxt_sun4", "JVM Options");
            sleep(1000);
            int emptyCount = getTableRowCountByValue("propertyForm:basicTable", "-Djava.security.manager", "col3:col1St", false);
            if (emptyCount != 0 ){
                String clickId = getTableRowByVal("propertyForm:basicTable", "-Djava.security.manager", "col3:col1St")+"col1:select";
                clickByIdAction(clickId);
                clickByIdAction("propertyForm:basicTable:topActionsGroup1:button1");
                waitforBtnDisable("propertyForm:basicTable:topActionsGroup1:button1");
                clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
                assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
            }
            sleep(1000);
            int beforeCount = getTableRowCount("propertyForm:basicTable");
            
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:"+  configName +":security:security_link");
            
            if (!driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:securityManagerProp:sun_checkbox380")).isSelected()){
                clickByIdAction("propertyForm:propertySheet:propertSectionTextField:securityManagerProp:sun_checkbox380");
            }
            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
            assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
            
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:"+  configName +":jvmSettings:jvmSettings_link");
            clickAndWait("propertyForm:javaConfigTab:jvmOptions");
            sleep(1000);
            int afterCount = getTableRowCount("propertyForm:basicTable");
            assertEquals(afterCount, beforeCount+1);
            
//            //delete security attribute if needed
//            emptyCount = getTableRowCountByValue("propertyForm:basicTable", "-Djava.security.manager", "col3:col1St", false);
//            if (emptyCount != 0 ){
//                String clickId = getTableRowByVal("propertyForm:basicTable", "-Djava.security.manager", "col3:col1St")+"col1:select";
//                clickByIdAction(clickId);
//                clickByIdAction("propertyForm:basicTable:topActionsGroup1:button1");
//                waitforBtnDisable("propertyForm:basicTable:topActionsGroup1:button1");
//                clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
//                isClassPresent("label_sun4");
//            }
        }
        deleteConfig("new-config");
    }

    @Test
    public void testNewSecurityRealm() {
        final String realmName = "TestRealm" + generateRandomString();
        final String contextName = "Context" + generateRandomString();

        createConfig("new-config");
        for (String configName : list) {
            createRealm(configName, realmName, contextName);
            
            //delete the related realm
            String clickId = getTableRowByValue("propertyForm:realmsTable", realmName, "col1")+"col0:select";
            clickByIdAction(clickId);
            clickByIdAction("propertyForm:realmsTable:topActionsGroup1:button1");
            closeAlertAndGetItsText();
            waitforBtnDisable("propertyForm:realmsTable:topActionsGroup1:button1");
        }
        deleteConfig("new-config");
    }

    @Test
    public void testAddUserToFileRealm() {
        final String userId = "user" + generateRandomString();
        final String password = "password" + generateRandomString();

        createConfig("new-config");
        for (String configName : list) {
            addUserToRealm(configName, "file", userId, password);
            
            //delete the added User for File Realm
            String clickId = getTableRowByValue("propertyForm:users", userId, "col1")+"col0:select";
            clickByIdAction(clickId);
            clickByIdAction("propertyForm:users:topActionsGroup1:button1");
            closeAlertAndGetItsText();
            waitforBtnDisable("propertyForm:users:topActionsGroup1:button1");
        }
        deleteConfig("new-config");
    }

    @Test
    public void testAddAuditModule() {
        final String auditModuleName = "auditModule" + generateRandomString();
        final String className = "org.glassfish.NonexistentModule";

        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:auditModules:auditModules_link");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", auditModuleName);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:classNameProp:ClassName", className);
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", auditModuleName, "col1");
            assertEquals(auditModuleName, getText(prefix + "col1:link"));

            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);

            assertTableRowCount("propertyForm:basicTable", count);

            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", auditModuleName);
        }
        deleteConfig("new-config");
    }

    @Test
    public void testAddJaccModule() {
        final String providerName = "testJaccProvider" + generateRandomString();
        final String policyConfig = "com.example.Foo";
        final String policyProvider = "com.example.Foo";
        final String propName = "propName";
        final String propValue = "propValue";
        final String propDescription = generateRandomString();

        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:jaccProviders:jaccProviders_link");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");

            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", providerName);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:policyConfigProp:PolicyConfig", policyConfig);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:policyProviderProp:PolicyProvider", policyProvider);

            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", propName);
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", propValue);
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", propDescription);

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", providerName, "col1");
            assertEquals(providerName, getText(prefix + "col1:link"));

            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);

            assertEquals(policyConfig, getValue("propertyForm:propertySheet:propertSectionTextField:policyConfigProp:PolicyConfig", "value"));
            assertEquals(policyProvider, getValue("propertyForm:propertySheet:propertSectionTextField:policyProviderProp:PolicyProvider", "value"));
            assertEquals(propName, getValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "value"));
            assertEquals(propValue, getValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value"));
            assertEquals(propDescription, getValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "value"));

            assertTableRowCount("propertyForm:basicTable", count);
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", providerName);
        }
        deleteConfig("new-config");
    }

    @Test
    public void testAddMessageSecurityConfiguration() {
        final String providerName = "provider" + generateRandomString();
        final String className = "com.example.Foo";

        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:messageSecurity:messageSecurity_link");
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:messageSecurity:SOAP:link");
            clickAndWait("propertyForm:msgSecurityTabs:providers");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");

            setFieldValue("propertyForm:propertySheet:providerConfSection:ProviderIdTextProp:ProviderIdText", providerName);
            setFieldValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName", className);
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", providerName, "col1");
            assertEquals(providerName, getText(prefix + "col1:authlayer"));

            String clickId = prefix + "col1:authlayer";
            clickByIdAction(clickId);

            assertEquals(className, getValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName", "value"));
            assertTableRowCount("propertyForm:basicTable", count);
            
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
            
            //delete security attribute if needed
            int emptyCount = getTableRowCountByValue("propertyForm:configs", providerName, "col1:authlayer", true);
            if (emptyCount != 0 ){
                clickId = getTableRowByValue("propertyForm:configs", providerName, "col1:authlayer")+"col0:select";
                clickByIdAction(clickId);
                clickByIdAction("propertyForm:configs:topActionsGroup1:button1");
                closeAlertAndGetItsText();
                waitforBtnDisable("propertyForm:configs:topActionsGroup1:button1");
            }
        }
        deleteConfig("new-config");
    }

    @Test
    public void testNewAdminPassword() {
        gotoDasPage();
        final String userPassword = "";

        clickAndWait("treeForm:tree:nodes:nodes_link");
        clickAndWait("propertyForm:domainTabs:adminPassword");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", userPassword);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", userPassword);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        closeAlertAndGetItsText();
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    /*
     * This test was add to test for regressions of GLASSFISH-14797
     */
    @Test
    public void testAddUserToRealmInRunningStandaloneInstance() {
        final String instanceName = "server" + generateRandomString();
        final String configName = instanceName + "-config";
        final String contextName = "Context" + generateRandomString();
        final String realmName = "newRealm";
        final String userName = "user" + generateRandomNumber();
        final StandaloneTest sat = new StandaloneTest();

        try {
            sat.createStandAloneInstance(instanceName);
            sat.startInstance(instanceName);

            createRealm(configName, realmName, contextName);
            addUserToRealm(configName, realmName, userName, "password");

            // Delete the user for good measure
            deleteUserFromRealm(configName, realmName, userName);
        } finally {
            sat.gotoStandaloneInstancesPage();
            sat.stopInstance(instanceName);
            sat.deleteStandAloneInstance(instanceName);
        }
    }

    /*
     * This test was added to test for GLASSFISH-16126
     * This test case need to be finished in the future
     */
//    @Test
//    public void testSecureAdministration() {
//        gotoDasPage();
//        clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
//        clickAndWait("propertyForm:propertyContentPage:secureAdmin");
//        if (driver.findElement(By.className("TtlTxt_sun4")).getText().equals("Secure Administration")) {
//            clickAndWait("form:propertyContentPage:topButtons:enableSecureAdminButton");
//            closeAlertAndGetItsText();
//            sleep(10000);
//            gotoDasPage();
//            clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
//            clickAndWait("propertyForm:propertyContentPage:secureAdmin");
//            selenium.click("form:propertyContentPage:topButtons:disableSecureAdminButton");
//            closeAlertAndGetItsText();
//            sleep(10000);
//        } 
//    }
    
//    //Need to be finished in the future
//    @Test
//    public void testRedirectAfterLogin() {
//        gotoDasPage();
//        final String newUser = "user" + generateRandomString();
//        final String realmName = "admin-realm";
//        final String newPass = generateRandomString();
//
//        try {
//            addUserToRealm("server-config", realmName, newUser, newPass);
//            // http://localhost:4848/common/help/help.jsf?contextRef=/resource/common/en/help/ref-developercommontasks.html
//            clickByIdAction("Masthead:logoutLink");
//            driver.close();
//            driver.get("http://localhost:4848/common/help/help.jsf?contextRef=/resource/common/en/help/ref-developercommontasks.html");
//            driver.close();
////            handleLogin(newUser, newPass, "The Common Tasks page provides shortcuts for common Administration Console tasks.");
//        } finally {
//            clickByIdAction("Masthead:logoutLink");
//            gotoDasPage();
////            handleLogin();
//            deleteUserFromRealm("server-config", realmName, newUser);
//        }
//    }

    public void createConfig(String configName) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        int emptyCount = getTableRowCountByValue("propertyForm:configs", "new-config", "col1:link", true);
        if (emptyCount == 0) {
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameProp:Name", configName);
            clickAndWait("propertyForm:propertyContentPage:topButtons:okButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", configName, "col1");
            assertEquals(configName, getText(prefix + "col1:link"));
        }
    }

    private void deleteConfig(String configName) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }
    
    public void createRealm(String configName, String realmName, String contextName) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link");
        clickAndWait("propertyForm:realmsTable:topActionsGroup1:newButton");
        setFieldValue("form1:propertySheet:propertySectionTextField:NameTextProp:NameText", realmName);
        Select select = new Select(driver.findElement(By.id("form1:propertySheet:propertySectionTextField:cp:Classname")));
        select.selectByVisibleText("com.sun.enterprise.security.auth.realm.file.FileRealm");
        setFieldValue("form1:fileSection:jaax:jaax", contextName);
        setFieldValue("form1:fileSection:keyFile:keyFile", "${com.sun.aas.instanceRoot}/config/testfile");
        clickAndWait("form1:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:realmsTable", realmName, "col1");
        assertEquals(realmName, getText(prefix + "col1:link"));
            
    }

    public void addUserToRealm(String configName, String realmName, String userName, String password) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link");
        
        String prefix = getTableRowByValue("propertyForm:realmsTable", realmName, "col1");
        assertEquals(realmName, getText(prefix + "col1:link"));

        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4","Edit Realm");
        clickAndWait("form1:propertyContentPage:manageUsersButton");
        clickAndWait("propertyForm:users:topActionsGroup1:newButton");

        setFieldValue("propertyForm:propertySheet:propertSectionTextField:userIdProp:UserId", userName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", password);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", password);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        
        String prefix1 = getTableRowByValue("propertyForm:users", userName, "col1");
        assertEquals(userName, getText(prefix1 + "col1:link"));

    }

    public void deleteUserFromRealm(String configName, String realmName, String userName) {
        clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link");
        
        String prefix = getTableRowByValue("propertyForm:realmsTable", realmName, "col1");
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);

        clickAndWait("form1:propertyContentPage:manageUsersButton");
        
      //delete security attribute if needed
      int emptyCount = getTableRowCountByValue("propertyForm:users", userName, "col1:link", true);
      if (emptyCount != 0 ){
          clickId = getTableRowByValue("propertyForm:users", userName, "col1:link")+"col0:select";
          clickByIdAction(clickId);
          clickByIdAction("propertyForm:users:topActionsGroup1:button1");
          closeAlertAndGetItsText();
          waitforBtnDisable("propertyForm:users:topActionsGroup1:button1");
      }
    }
}
