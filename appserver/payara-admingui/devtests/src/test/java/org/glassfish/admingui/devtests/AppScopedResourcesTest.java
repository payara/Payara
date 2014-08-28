/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author sumasri
 */
public class AppScopedResourcesTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_APPLICATIONS = "Applications can be enterprise or web applications, or various kinds of modules.";
    private static final String TRIGGER_EDIT_APPLICATION = "Modify an existing application or module.";
    private static final String TRIGGER_RESOURCES_APPLICATION = "View application scoped resources for the application.";

    private static final String ELEMENT_APP_NAME = "form:war:psection:nameProp:appName";
    private static final String ELEMENT_UNDEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:button1";
    private static final String ELEMENT_DEPLOY_TABLE = "propertyForm:deployTable";
    private static final String ELEMENT_UPLOAD_BUTTON = "form:title:topButtons:uploadButton";
    private static final String ELEMENT_FILE_FIELD = "form:sheet1:section1:prop1:fileupload";
    private static final String ELEMENT_DEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:deployButton";
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
        goToApplicationResourcesTab(applicationName);

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
        clusterTest.deleteAllClusters();
        
        clickAndWait("treeForm:tree:applications:applications_link", TRIGGER_APPLICATIONS);
        int preCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);

        // hrm
        clickAndWaitForElement(ELEMENT_DEPLOY_BUTTON, ELEMENT_FILE_FIELD);
        File war = new File("src/test/resources/JavaEEApp.ear");
        try {
            selectFile(ELEMENT_FILE_FIELD, war.toURL().toString());
        } catch (MalformedURLException e) {
           e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        assertEquals("JavaEEApp", getFieldValue(ELEMENT_APP_NAME));

        setFieldValue(ELEMENT_APP_NAME, applicationName);

        clickAndWait(ELEMENT_UPLOAD_BUTTON, TRIGGER_APPLICATIONS);
        String conf = "";
        if (isAlertPresent()) {
            conf = getAlertText();
        }
        int postCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount < postCount);
    }

    private void undeployApp(String applicationName) {
        // Undeploy application
        clickAndWait("treeForm:tree:applications:applications_link", TRIGGER_APPLICATIONS);
        int preCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        int appCount = this.getTableRowCountByValue(ELEMENT_DEPLOY_TABLE, applicationName, "col1");
        if (appCount == 0) {
            return;
        }
        chooseOkOnNextConfirmation();
        selectTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName);
        pressButton(ELEMENT_UNDEPLOY_BUTTON);
        getConfirmation();
        waitForPageLoad(applicationName, true);
        int postUndeployCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount > postUndeployCount);
    }

    private void goToApplicationResourcesTab(String appName) {
        clickAndWait(ELEMENT_APPLICATIONS, TRIGGER_APPLICATIONS);
        waitForPageLoad(appName, 60000);
        clickAndWait(getLinkIdByLinkText(ELEMENT_DEPLOY_TABLE, appName), TRIGGER_EDIT_APPLICATION);
        clickAndWait(ELEMENT_APPLICATION_RESOURCES_TAB, TRIGGER_RESOURCES_APPLICATION);
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
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), JdbcTest.TRIGGER_EDIT_JDBC_CONNECTION_POOL);
        setFieldValue("propertyForm:sheet:generalSheet:descProp:desc", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        //JDBC Pool Advanced Page
        clickAndWait("propertyForm:jdbcPoolSet:advanceTab", JdbcTest.TRIGGER_ADVANCE_JDBC_CONNECTION_POOL);
        setFieldValue("propertyForm:propertySheet:connectionPropertySheet:p1:va", "1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        //JDBC Pool Properties
        clickAndWait("propertyForm:jdbcPoolSet:propertyTab", JdbcTest.TRIGGER_PROPS_JDBC_CONNECTION_POOL);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testJDBCResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //JDBC Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), JdbcTest.TRIGGER_EDIT_JDBC_RESOURCE);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testCustomResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Custom Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), JndiTest.TRIGGER_EDIT_CUSTOM_RESOURCE);
        setFieldValue("form1:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("form1:basicTable", count);
        
        clickAndWait("form1:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testExternalResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //JNDI Enternal Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), JndiTest.TRIGGER_EDIT_EXTERNAL_RESOURCE);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("form:basicTable", count);
        
        clickAndWait("form:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testMailResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Mail Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), JavaMailTest.TRIGGER_EDIT_JAVAMAIL_SESSION);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testWorkSecurityMap(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Work Security Map Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), WorkSecurityMapTest.TRIGGER_EDIT_WORK_SECURITY_MAP);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testConnectorPool(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Connector Pool general page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), ConnectorsTest.TRIGGER_EDIT_CONNECTOR_CONNECTION_POOL);
        setFieldValue("propertyForm:propertySheet:generalPropertySheet:descProp:desc", resName+" description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        //Connector Pool Advanced Page
        clickAndWait("propertyForm:connectorPoolSet:advanceTab", ConnectorsTest.TRIGGER_ADVANCE_CONNECTOR_CONNECTION_POOL);
        setFieldValue("propertyForm:connectionPropertySheet:p1:va", "1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        //Connector Pool Properties
        clickAndWait("propertyForm:connectorPoolSet:propertyTab", ConnectorsTest.TRIGGER_PROPS_CONNECTOR_CONNECTION_POOL);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testConnectorResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Connector Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), ConnectorsTest.TRIGGER_EDIT_CONNECTOR_RESOURCE);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descProp:desc", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void testResourceAdapterConfig(String appName, String resName) {        
        java.util.List<String> resAdapterConfigIds = getTableRowsByValue(ELEMENT_APP_RESOURCES_TABLE, resName, "resName");
        //Resource Adapter config Edit Page
        for (String resAdapterConfigId : resAdapterConfigIds) {
            goToApplicationResourcesTab(appName);
            clickAndWait(resAdapterConfigId, ResourceAdapterConfigsTest.TRIGGER_EDIT_RESOURCE_ADAPTER_CONFIG);
            int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
            assertTableRowCount("propertyForm:basicTable", count);
            
            clickAndWait("propertyForm:proprtyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
        }
    }

    private void testAdminObjectResource(String appName, String resName, String appScope) {
        resName = getResName(resName, appScope);
        goToApplicationResourcesTab(appName);
        //Admin Object Resource Edit Page
        clickAndWait(getLinkIdByLinkText(ELEMENT_APP_RESOURCES_TABLE, resName), AdminObjectTest.TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", resName+" description");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_RESOURCES_APPLICATION);
    }

    private void monitoringAppScopedResource(String resName, String appScope) {
        MonitoringTest monitorTest = new MonitoringTest();
        resName = getResName(resName, appScope);
        monitorTest.appScopedResourcesMonitoring("server", "server", resName);
    }
}
