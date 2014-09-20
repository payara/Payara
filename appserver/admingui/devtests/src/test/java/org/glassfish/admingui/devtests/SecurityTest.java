/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SecurityTest extends BaseSeleniumTestClass {
    public static final String TRIGGER_NEW_REALM = "i18nc.realm.NewPageHelp";
    public static final String TRIGGER_SECURITY_REALMS = "i18nc.realm.PageHelp";
    public static final String TRIGGER_EDIT_REALM = "i18nc.realm.EditPageTitleHelp";
    public static final String TRIGGER_FILE_USERS = "i18nc.manageUsers.TablePageHelp";
    public static final String TRIGGER_NEW_FILE_REALM_USER = "i18nc.manageUsers.NewPageTitle";
    public static final String TRIGGER_AUDIT_MODULES = "com.sun.enterprise.security.Audit";
    //"Use audit modules to develop an audit trail of all authentication and authorization decisions.";
    public static final String TRIGGER_NEW_AUDIT_MODULE = "i18nc.auditModule.NewPageTitle";
    public static final String TRIGGER_EDIT_AUDIT_MODULE = "i18nc.auditModule.EditPageTitle";
    public static final String TRIGGER_JACC_PROVIDERS = "i18nc.jacc.PageHelp";
    public static final String TRIGGER_NEW_JACC_PROVIDER = "i18nc.jacc.NewPageTitle";
    public static final String TRIGGER_EDIT_JACC_PROVIDER = "i18nc.jacc.EditTitle";
    public static final String TRIGGER_MESSAGE_SECURITY_CONFIGURATIONS = "i18nc.msgSecurity.ListPageTitle";
    public static final String TRIGGER_NEW_MESSAGE_SECURITY_CONFIGURATION = "i18nc.headings.NewMsgSecurity";
    public static final String TRIGGER_EDIT_MESSAGE_SECURITY_CONFIGURATION = "i18nc.msgSecurity.EditMsgSecurity";
    public static final String TRIGGER_EDIT_PROVIDER_CONFIGURATION = "i18nc.msgProvider.EditPageTitle";
    public static final String TRIGGER_PROVIDER_CONFIGURATION = "i18nc.msgSecProvider.TableTitle";
    public static final String TRIGGER_NEW_PROVIDER_CONFIGURATION = "i18nc.msgSecProvider.NewPageTitle";
    public static final String TRIGGER_GENERAL_INFORMATION = "i18n.instance.GeneralTitle";
    public static final String TRIGGER_SECURE_ADMINISTRATION = "i18nc.security.secureAdmin";
    public static final String TRIGGER_ADMIN_ALIAS = "i18nc.security.secureAdmin.adminalias";
    public static final String TRIGGER_RESTART_DOMAIN = "i18n.restart.RestartHeading";
    public static final String ADMIN_PWD_DOMAIN_ATTRIBUTES = "i18nc.domain.DomainAttrsPageTitle";
    public static final String ADMIN_PWD_NEW_ADMINPWD = "i18nc.domain.AdminPasswordTitle";

    public static final String JVM_CONFIG = "i18nc.jvm.GeneralPageHelp";
    public static final String JVM_OPTION = "i18nc.jvmOptions.PageHelp";
    public static final String SECURITY_MGR = "i18nc.security.SecurityPageHelp";

    private static final String TRIGGER_CONFIGURATION = "i18nc.configurations.PageTitleHelp";
    private static final String TRIGGER_NEW_CONFIGURATION = "i18nc.configurations.NewPageTitle";
    ArrayList<String> list = new ArrayList(); {list.add("server-config"); list.add("new-config");}


//    @Test
    // TODO: The page has a component without an explicit ID. Disabling the test for now.
    public void testSecurityPage() {

        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":jvmSettings:jvmSettings_link", JVM_CONFIG);
            clickAndWait("propertyForm:javaConfigTab:jvmOptions", JVM_OPTION);
            int beforeCount = getTableRowCount("propertyForm:basicTable");
            clickAndWait("treeForm:tree:configurations:server-config:security:security_link", SECURITY_MGR);
            markCheckbox("propertyForm:propertySheet:propertSectionTextField:securityManagerProp:sun_checkbox133"); // TODO: Give this component an ID
            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
            clickAndWait("treeForm:tree:configurations:server-config:jvmSettings:jvmSettings_link", JVM_CONFIG);
            clickAndWait("propertyForm:javaConfigTab:jvmOptions", JVM_OPTION);
            int afterCount = getTableRowCount("propertyForm:basicTable");
            assertEquals(afterCount, beforeCount+1);
        }
    }

    @Test
    public void testNewSecurityRealm() {
        final String realmName = "TestRealm" + generateRandomString();
        final String contextName = "Context" + generateRandomString();

        createConfig("new-config");
        for (String configName : list) {
            createRealm(configName, realmName, contextName);
            deleteRow("propertyForm:realmsTable:topActionsGroup1:button1", "propertyForm:realmsTable", realmName);
        }
    }

    @Test
    public void testAddUserToFileRealm() {
        final String userId = "user" + generateRandomString();
        final String password = "password" + generateRandomString();

        createConfig("new-config");
        for (String configName : list) {
            addUserToRealm(configName, "file", userId, password);

            deleteRow("propertyForm:users:topActionsGroup1:button1", "propertyForm:users", userId);
        }
    }

    @Test
    public void testAddAuditModule() {
        final String auditModuleName = "auditModule" + generateRandomString();
        final String className = "org.glassfish.NonexistentModule";

        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:auditModules:auditModules_link", TRIGGER_AUDIT_MODULES);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_AUDIT_MODULE);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", auditModuleName);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:classNameProp:ClassName", className);
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_AUDIT_MODULES);
            assertTrue(isTextPresent(auditModuleName));

            clickAndWait(getLinkIdByLinkText("propertyForm:configs", auditModuleName), TRIGGER_EDIT_AUDIT_MODULE);
            assertTableRowCount("propertyForm:basicTable", count);

            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_AUDIT_MODULES);

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", auditModuleName);
        }
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
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:jaccProviders:jaccProviders_link", TRIGGER_JACC_PROVIDERS);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JACC_PROVIDER);

            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", providerName);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:policyConfigProp:PolicyConfig", policyConfig);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:policyProviderProp:PolicyProvider", policyProvider);

            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", propName);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", propValue);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", propDescription);

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_JACC_PROVIDERS);
            assertTrue(tableContainsRow("propertyForm:configs", "col1", providerName));

            clickAndWait(getLinkIdByLinkText("propertyForm:configs", providerName), TRIGGER_EDIT_JACC_PROVIDER);
            assertEquals(policyConfig, getFieldValue("propertyForm:propertySheet:propertSectionTextField:policyConfigProp:PolicyConfig"));
            assertEquals(policyProvider, getFieldValue("propertyForm:propertySheet:propertSectionTextField:policyProviderProp:PolicyProvider"));
            assertEquals(propName, getFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St"));
            assertEquals(propValue, getFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St"));
            assertEquals(propDescription, getFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St"));

            assertTableRowCount("propertyForm:basicTable", count);
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JACC_PROVIDERS);

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", providerName);
        }
    }

    @Test
    public void testAddMessageSecurityConfiguration() {
        final String providerName = "provider" + generateRandomString();
        final String className = "com.example.Foo";

        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:messageSecurity:messageSecurity_link", TRIGGER_MESSAGE_SECURITY_CONFIGURATIONS);
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:messageSecurity:SOAP:link", TRIGGER_EDIT_MESSAGE_SECURITY_CONFIGURATION);
            clickAndWait("propertyForm:msgSecurityTabs:providers", TRIGGER_PROVIDER_CONFIGURATION);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_PROVIDER_CONFIGURATION);

            setFieldValue("propertyForm:propertySheet:providerConfSection:ProviderIdTextProp:ProviderIdText", providerName);
            setFieldValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName", className);
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_PROVIDER_CONFIGURATION);
            assertTrue(isTextPresent(providerName));
            clickAndWait(getLinkIdByLinkText("propertyForm:configs", providerName), TRIGGER_EDIT_PROVIDER_CONFIGURATION);
            // Case Added for Issue 15711
            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
            assertEquals(className, getFieldValue("propertyForm:propertySheet:providerConfSection:ClassNameProp:ClassName"));
            assertTableRowCount("propertyForm:basicTable", count);
        }
    }

    @Test
    public void testNewAdminPassword() {
        final String userPassword = "";

        clickAndWait("treeForm:tree:nodes:nodes_link", ADMIN_PWD_DOMAIN_ATTRIBUTES);
        clickAndWait("propertyForm:domainTabs:adminPassword", ADMIN_PWD_NEW_ADMINPWD);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", userPassword);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", userPassword);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
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

            sat.deleteStandAloneInstance(instanceName);
        }
    }

    /*
     * This test was added to test for GLASSFISH-16126
     */
    @Test
    public void testSecureAdministration() {
        clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_GENERAL_INFORMATION);
        clickAndWait("propertyForm:propertyContentPage:secureAdmin", TRIGGER_SECURE_ADMINISTRATION);
        if (isTextPresent(TRIGGER_ADMIN_ALIAS)) {
            selenium.click("form:propertyContentPage:topButtons:enableSecureAdminButton");
            getConfirmation();
            waitForPageLoad(TRIGGER_RESTART_DOMAIN, TIMEOUT);
            sleep(10000);
            clickAndWait("link=here", "Common Tasks");
            clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_GENERAL_INFORMATION);
            clickAndWait("propertyForm:propertyContentPage:secureAdmin", TRIGGER_SECURE_ADMINISTRATION);
            selenium.click("form:propertyContentPage:topButtons:disableSecureAdminButton");
            getConfirmation();
            waitForPageLoad(TRIGGER_RESTART_DOMAIN, TIMEOUT);
            sleep(10000);
        } else {
            selenium.click("form:propertyContentPage:topButtons:disableSecureAdminButton");
            getConfirmation();
            waitForPageLoad(TRIGGER_RESTART_DOMAIN, TIMEOUT);
            sleep(10000);
        }
    }

    @Test
    public void testRedirectAfterLogin() {
        final String newUser = "user" + generateRandomString();
        final String realmName = "admin-realm";
        final String newPass = generateRandomString();

        try {
            addUserToRealm("server-config", realmName, newUser, newPass);
            // http://localhost:4848/common/help/help.jsf?contextRef=/resource/common/en/help/ref-developercommontasks.html
            reset();
            pressButton("Masthead:logoutLink");
            waitForLoginPageLoad(30);
            open ("http://localhost:4848/common/help/help.jsf?contextRef=/resource/common/en/help/ref-developercommontasks.html");
            handleLogin(newUser, newPass, "The Common Tasks page provides shortcuts for common Administration Console tasks.");
        } finally {
            reset();
            pressButton("Masthead:logoutLink");
            waitForLoginPageLoad(30);
            handleLogin();
            deleteUserFromRealm("server-config", realmName, newUser);
        }
    }

    public void createConfig(String configName) {
        clickAndWait("treeForm:tree:configurations:configurations_link", TRIGGER_CONFIGURATION);
        if (!isTextPresent("new-config")) {
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_CONFIGURATION);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameProp:Name", configName);
            clickAndWait("propertyForm:propertyContentPage:topButtons:okButton", TRIGGER_CONFIGURATION);
            assertTrue(isTextPresent(configName));
        }
    }

    public void createRealm(String configName, String realmName, String contextName) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link", TRIGGER_SECURITY_REALMS);
            clickAndWait("propertyForm:realmsTable:topActionsGroup1:newButton", TRIGGER_NEW_REALM);
            setFieldValue("form1:propertySheet:propertySectionTextField:NameTextProp:NameText", realmName);
            selectDropdownOption("form1:propertySheet:propertySectionTextField:cp:Classname", "com.sun.enterprise.security.auth.realm.file.FileRealm");
            setFieldValue("form1:fileSection:jaax:jaax", contextName);
            setFieldValue("form1:fileSection:keyFile:keyFile", "${com.sun.aas.instanceRoot}/config/testfile");
            clickAndWait("form1:propertyContentPage:topButtons:newButton", TRIGGER_SECURITY_REALMS);
            assertTrue(isTextPresent(realmName));
    }

    public void addUserToRealm(String configName, String realmName, String userName, String password) {
        reset();
        clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link", TRIGGER_SECURITY_REALMS);
        clickAndWait(getLinkIdByLinkText("propertyForm:realmsTable", realmName), TRIGGER_EDIT_REALM);

        clickAndWait("form1:propertyContentPage:manageUsersButton", TRIGGER_FILE_USERS);
        clickAndWait("propertyForm:users:topActionsGroup1:newButton", TRIGGER_NEW_FILE_REALM_USER);

        setFieldValue("propertyForm:propertySheet:propertSectionTextField:userIdProp:UserId", userName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", password);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", password);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_FILE_USERS);
        assertTrue(isTextPresent(userName));

    }

    public void deleteUserFromRealm(String configName, String realmName, String userName) {
        reset();
        clickAndWait("treeForm:tree:configurations:" + configName + ":security:realms:realms_link", TRIGGER_SECURITY_REALMS);
        clickAndWait(getLinkIdByLinkText("propertyForm:realmsTable", realmName), TRIGGER_EDIT_REALM);

        clickAndWait("form1:propertyContentPage:manageUsersButton", TRIGGER_FILE_USERS);
        deleteRow("propertyForm:users:topActionsGroup1:button1", "propertyForm:users", userName);
    }
}
