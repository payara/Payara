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

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author jeremy lv
 */
public class AppScopedResourcesTest extends BaseSeleniumTestClass {

    private static final String ELEMENT_EARAPP_NAME = "form:ear:psection:nameProp:appName";
    private static final String ELEMENT_UNDEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:button1";
    private static final String ELEMENT_DEPLOY_TABLE = "propertyForm:deployTable";
    private static final String ELEMENT_UPLOAD_BUTTON = "form:title:topButtons:uploadButton";
    private static final String ELEMENT_FILE_FIELD = "form:sheet1:section1:prop1:fileupload";
    private static final String ELEMENT_APPLICATIONS = "treeForm:tree:applications:applications_link";
    private static final String ELEMENT_APPLICATION_RESOURCES_TAB = "propertyForm:appGeneralTabs:resourcesTab";
    private static final String ELEMENT_APP_RESOURCES_TABLE = "propertyForm:appScopedResources";
    
    @Test
    public void testAppScopedResApp() {
        final String applicationName = generateRandomString();
        deployApp(applicationName); 
        undeployApp(applicationName);
    }

    @Test
    public void testJdbcAppScopedresources() {
        final String applicationName = generateRandomString();
        try {
        deployApp(applicationName);

        testJDBCPool(applicationName, "jdbcPool", "app");
        testJDBCPool(applicationName, "jdbcPool", "module");

        testJDBCResource(applicationName, "jdbcRes", "app");
        testJDBCResource(applicationName, "jdbcRes", "module");
        
        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }

    @Test
    public void testConnectorAppScopedresources() {
        final String applicationName = generateRandomString();
        try{
        deployApp(applicationName);

        testConnectorPool(applicationName, "connectorPool", "app");
        testConnectorPool(applicationName, "connectorPool", "module");

        testConnectorResource(applicationName, "connectorRes", "app");
        testConnectorResource(applicationName, "connectorRes", "module");

        testAdminObjectResource(applicationName, "jms/adminObjectRes", "app");
        testAdminObjectResource(applicationName, "jms/adminObjectRes", "module");

        testWorkSecurityMap(applicationName, "workSecurityMap", "app");
        testWorkSecurityMap(applicationName, "workSecurityMap", "module");

        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }

    @Test
    public void testJndiAppScopedresources() {
        final String applicationName = generateRandomString();
        try{
        deployApp(applicationName);

        testCustomResource(applicationName, "customRes", "app");
        testCustomResource(applicationName, "customRes", "module");

        testExternalResource(applicationName, "externalRes", "app");
        testExternalResource(applicationName, "externalRes", "module");

        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }

    @Test
    public void testJavaMailAppScopedresources() {
        final String applicationName = generateRandomString();
        try{
        deployApp(applicationName);

        testMailResource(applicationName, "mailRes", "app");
        testMailResource(applicationName, "mailRes", "module");

        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }

    @Test
    public void testResourceAdapterConfigAppScopedresources() {
        final String applicationName = generateRandomString();
        try{
        deployApp(applicationName);

        testResourceAdapterConfig(applicationName, "jmsra");

        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }

    @Test
    public void testMonitoringAppScopedresources() {
        final String applicationName = generateRandomString();
        try{
        deployApp(applicationName);

        monitoringAppScopedResource("connectorPool", "module");
        monitoringAppScopedResource("jdbcPool", "module");

        undeployApp(applicationName);
        }catch(Exception e) {
            undeployApp(applicationName);
        }
    }
    
    public void deployApp(String applicationName) {
        
        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();
        
        clickAndWait("treeForm:tree:applications:applications_link");
        sleep(1000);
        int initCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);
        if(initCount != 0){
            clickByIdAction("propertyForm:deployTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
            clickByIdAction(ELEMENT_UNDEPLOY_BUTTON);
            closeAlertAndGetItsText();
            waitForAlertProcess("modalBody");
            waitforBtnDisable(ELEMENT_UNDEPLOY_BUTTON);
        }
        sleep(1000);
        int preCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);

        //start to deploy applications
        driver.get(baseUrl + "common/applications/uploadFrame.jsf");
        driver.findElement(By.id("form:sheet1:section1:prop1:uploadRdBtn:uploadRdBtn_label"));
        File war = new File("src/test/resources/JavaEEApp.ear");
        driver.findElement(By.id(ELEMENT_FILE_FIELD)).sendKeys(war.getAbsoluteFile().toString());
        
        assertEquals("JavaEEApp", getValue(ELEMENT_EARAPP_NAME, "value"));

        setFieldValue(ELEMENT_EARAPP_NAME, applicationName);
        clickAndWait(ELEMENT_UPLOAD_BUTTON);
        
        //add some sleep time here to wait for the webdriver element located
        sleep(10000);
        gotoDasPage();
        clickAndWait("treeForm:tree:applications:applications_link");
        sleep(1000);
        int postCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount < postCount);
    }

    private void undeployApp(String applicationName) {
        // Undeploy application
        gotoDasPage();
        clickAndWait("treeForm:tree:applications:applications_link");
        int preCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);
        int appCount = getTableRowCountByValue(ELEMENT_DEPLOY_TABLE, applicationName, "col1");
        if (appCount == 0) {
            return;
        }
        String prefix = getTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName, "col1");
        String selectId = prefix + "col0:select";
        clickByIdAction(selectId);
        clickAndWait(ELEMENT_UNDEPLOY_BUTTON);
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        waitforBtnDisable(ELEMENT_UNDEPLOY_BUTTON);
        sleep(1000);
        int postUndeployCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount > postUndeployCount);
    }

    private void goToApplicationResourcesTab(String appName) {
        clickAndWait(ELEMENT_APPLICATIONS);
        
        String prefix = getTableRowByValue(ELEMENT_DEPLOY_TABLE, appName, "col1");
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Application");
        clickAndWait(ELEMENT_APPLICATION_RESOURCES_TAB);
        waitForElementPresent("TtlTxt_sun4", "Application Scoped Resources");
    }

    public String getResName(String resName, String appScope) {
        if(appScope.equals("app")) {
            resName = "java:app/" + resName;
        } else if(appScope.equals("module")) {
            resName = "java:module/" + resName;
        }
        return resName;
    }

    private void testJDBCPool(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //JDBC Pool general page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit JDBC Connection Pool");
        setFieldValue("propertyForm:sheet:generalSheet:descProp:desc", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        //JDBC Pool Advanced Page
        clickAndWait("propertyForm:jdbcPoolSet:advanceTab");
        waitForElementPresent("TtlTxt_sun4", "Edit JDBC Connection Pool Advanced Attributes");
        setFieldValue("propertyForm:propertySheet:connectionPropertySheet:p1:va", "1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        //JDBC Pool Properties
        clickAndWait("propertyForm:jdbcPoolSet:propertyTab");
        waitForElementPresent("TtlTxt_sun4", "Edit JDBC Connection Pool Properties");
        sleep(1000);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testJDBCResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //JDBC Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit JDBC Resource");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        sleep(1000);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testCustomResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Custom Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Custom Resource");
        setFieldValue("form1:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        sleep(5000);
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("form1:basicTable", count);
        
        clickAndWait("form1:propertyContentPage:topButtons:cancelButton");
    }

    private void testExternalResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //JNDI Enternal Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit External Resource");
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("form:basicTable", count);
        
        clickAndWait("form:propertyContentPage:topButtons:cancelButton");
    }

    private void testMailResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Mail Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit JavaMail Session");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testWorkSecurityMap(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Work Security Map Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Work Security Map");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testConnectorPool(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Connector Pool general page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Connector Connection Pool");
        setFieldValue("propertyForm:propertySheet:generalPropertySheet:descProp:desc", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        //Connector Pool Advanced Page
        clickAndWait("propertyForm:connectorPoolSet:advanceTab");
        waitForElementPresent("TtlTxt_sun4", "Edit Connector Connection Pool Advanced Attributes");
        setFieldValue("propertyForm:connectionPropertySheet:p1:va", "1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        //Connector Pool Properties
        clickAndWait("propertyForm:connectorPoolSet:propertyTab");
        waitForElementPresent("TtlTxt_sun4", "Edit Connector Connection Pool Properties");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testConnectorResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Connector Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Connector Resource");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void testResourceAdapterConfig(String appName, String resName) {
        goToApplicationResourcesTab(appName);
        java.util.List<String> resAdapterConfigIds = getTableRowsByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName");
        //Resource Adapter config Edit Page
        for (String resAdapterConfigId : resAdapterConfigIds) {
            goToApplicationResourcesTab(appName);
            String clickId = resAdapterConfigId + ":resName:resNameCol";
            clickAndWait(clickId);
            waitForElementPresent("TtlTxt_sun4", "Edit Resource Adapter Config");
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addButton");
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
            sleep(500);
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
            assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
            assertTableRowCount("propertyForm:basicTable", count);
            
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
        }
    }

    private void testAdminObjectResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Admin Object Resource Edit Page
        String prefix = getTableRowByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName:resNameCol");
        String clickId = prefix + "resName:resNameCol";
        clickByIdAction(clickId);
        waitForElementPresent("TtlTxt_sun4", "Edit Admin Object Resource");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
    }

    private void monitoringAppScopedResource(String resName, String appScope) {
        MonitoringTest monitorTest = new MonitoringTest();
        resName = getResName(resName, appScope);
        monitorTest.appScopedResourcesMonitoring("server", "server", resName);
    }
}
