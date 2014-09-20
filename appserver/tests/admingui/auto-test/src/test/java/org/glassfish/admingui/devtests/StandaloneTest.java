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
import static org.junit.Assert.assertTrue;

/**
 *
 * @author jeremy_lv
 */
public class StandaloneTest  extends BaseSeleniumTestClass {
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_STOPPED = "Stopped";

    public static final String ID_INSTANCE_TABLE_NEW_BUTTON = "propertyForm:instancesTable:topActionsGroup1:newButton";
    public static final String ID_INSTANCE_TABLE_DELETE_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button1";
    public static final String ID_INSTANCE_TABLE_START_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button2";
    public static final String ID_INSTANCE_TABLE_STOP_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button3";
    public static final String ID_INSTANCE_TABLE = "propertyForm:instancesTable";
    public static final String ID_INSTANCE_PROP_TAB = "propertyForm:standaloneInstanceTabs:standaloneProp";

    public static final String ID_INSTANCE_NAME_TEXT = "propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText";
    public static final String ID_INSTANCE_NODE_TEXT = "propertyForm:propertySheet:propertSectionTextField:node:node" ;
    public static final String ID_INSTANCE_CONFIG_SELECT = "propertyForm:propertySheet:propertSectionTextField:configProp:Config" ;
    public static final String ID_INSTANCE_CONFIG_OPTION = "propertyForm:propertySheet:propertSectionTextField:configOptionProp:optC";
    public static final String ID_INSTANCE_NEW_PAGE_BUTTON = "propertyForm:propertyContentPage:topButtons:newButton" ;

    public static final String INSTANCE_PREFIX = "standAlone" ;
    public static final String NODE_NAME = "localhost-domain1" ;
    public static final String DEFAULT_WEIGHT = "100" ;

    @Test
    public void testCreateStartStopAndDeleteStandaloneInstance() {
        String instanceName = INSTANCE_PREFIX + generateRandomString();
        createStandAloneInstance(instanceName);

        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertEquals(instanceName, getText(prefix + "col1:link"));
        assertEquals(instanceName+"-config", getText(prefix + "col3:configlink"));
        assertEquals(NODE_NAME, getText(prefix + "col5:nodeAgentlink"));
        assertTrue(getText(prefix + "col6").endsWith(STATUS_STOPPED));
        assertEquals(DEFAULT_WEIGHT, getValue(prefix + "col2:weight", "value"));

        startInstance(instanceName);
        assertTrue(getText(prefix + "col6").endsWith(STATUS_RUNNING));

        stopInstance(instanceName);
        assertTrue(getText(prefix + "col6").endsWith(STATUS_STOPPED));

        deleteStandAloneInstance(instanceName);
    }

    @Test
    public void testProperties() {
        String instanceName = INSTANCE_PREFIX + generateRandomString();
        createStandAloneInstance(instanceName);

        String clickId = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1")+"col1:link";
        clickByIdAction(clickId);
        clickAndWait(ID_INSTANCE_PROP_TAB);
        int sysPropCount = addTableRow("propertyForm:sysPropsTable", "propertyForm:sysPropsTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:overrideValCol:overrideVal", "foo=bar");
        // FIXME: The app needs to be fixed here. should show success message
        clickAndWait("propertyForm:clusterSysPropsPage:topButtons:topButtons:saveButton");
        assertTrue(driver.findElement(By.className("label_sun4")).getText().equals("New values successfully saved."));

        // Go to instance props page
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:instanceProps"); // FIXME

        int instancePropCount = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "foo=bar");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(driver.findElement(By.className("label_sun4")).getText().equals("New values successfully saved."));

        // Verify that properties were persisted
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:configProps");

        assertTableRowCount("propertyForm:sysPropsTable", sysPropCount);
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:instanceProps"); // FIXME
        assertTableRowCount("propertyForm:basicTable", instancePropCount);

        deleteStandAloneInstance(instanceName);
    }

    @Test
    public void testStandaloneInstanceResourcesPage() {
        final String jndiName = "jdbcResource"+generateRandomString();
        String target = INSTANCE_PREFIX + generateRandomString();
        final String description = "devtest test for standalone instance->resources page- " + jndiName;
        final String tableID = "propertyForm:resourcesTable";

        JdbcTest jdbcTest = new JdbcTest();
        jdbcTest.createJDBCResource(jndiName, description, target, "standalone");

        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, target, "col1");
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        clickAndWait("propertyForm:standaloneInstanceTabs:resources");
        String resourcePrefix = getTableRowByValue(tableID, jndiName, "col1");
        assertTrue(isTextPresent(resourcePrefix, jndiName, tableID));

        int jdbcCount = getTableRowCountByValue(tableID, "JDBC Resources", "col3:type");
        int customCount = getTableRowCountByValue(tableID, "Custom Resources", "col3:type");

        EnterpriseServerTest adminServerTest = new EnterpriseServerTest();
        Select select = new Select(driver.findElement(By.id("propertyForm:resourcesTable:topActionsGroup1:filter_list")));
        select.selectByVisibleText("Custom Resources");

        adminServerTest.waitForTableRowCount(tableID, customCount);

        select = new Select(driver.findElement(By.id("propertyForm:resourcesTable:topActionsGroup1:filter_list")));
        select.selectByVisibleText("JDBC Resources");
        adminServerTest.waitForTableRowCount(tableID, jdbcCount);

        clickId = getTableRowByValue(tableID, jndiName, "col1") + "col1:link";
        clickByIdAction(clickId);
        waitForButtonEnabled("propertyForm:propertyContentPage:topButtons:saveButton");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");

        jdbcTest.deleteJDBCResource(jndiName, target, "standalone");
    }

    public void createStandAloneInstance(String instanceName){
        gotoStandaloneInstancesPage();
        clickAndWait(ID_INSTANCE_TABLE_NEW_BUTTON);
        setFieldValue(ID_INSTANCE_NAME_TEXT, instanceName);
        Select select = new Select(driver.findElement(By.id(ID_INSTANCE_NODE_TEXT)));
        select.selectByVisibleText(NODE_NAME);
        Select select1 = new Select(driver.findElement(By.id(ID_INSTANCE_CONFIG_SELECT)));
        select1.selectByVisibleText("default-config");
        clickByIdAction(ID_INSTANCE_CONFIG_OPTION);
        clickAndWait(ID_INSTANCE_NEW_PAGE_BUTTON);
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertEquals(instanceName, getText(prefix + "col1:link"));
    }

    public void deleteStandAloneInstance(String instanceName) {
        gotoStandaloneInstancesPage();
        String clickId = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1")+"col0:select";
        clickByIdAction(clickId);
        clickByIdAction(ID_INSTANCE_TABLE_DELETE_BUTTON);
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
    }

    public void deleteAllStandaloneInstances() {
        gotoStandaloneInstancesPage();
        if (getTableRowCount(ID_INSTANCE_TABLE) == 0) {
            return;
        }

        //Disable all of the standalone instance
        clickByIdAction("propertyForm:instancesTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("propertyForm:instancesTable:topActionsGroup1:button3");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");

        // Delete all instances
        clickByIdAction("propertyForm:instancesTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("propertyForm:instancesTable:topActionsGroup1:button1");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
    }

    public void startInstance(String instanceName) {
        String clickId = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1")+"col0:select";
        clickByIdAction(clickId);
        clickByIdAction(ID_INSTANCE_TABLE_START_BUTTON);
        assertTrue(closeAlertAndGetItsText().matches("^Start the selected GlassFish Server instances[\\s\\S]$"));
        waitForAlertProcess("modalBody");
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertTrue(getText(prefix + "col6").endsWith(STATUS_RUNNING));
    }

    public void stopInstance(String instanceName) {
        String clickId = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1")+"col0:select";
        clickByIdAction(clickId);
        clickByIdAction(ID_INSTANCE_TABLE_STOP_BUTTON);
        assertTrue(closeAlertAndGetItsText().matches("^Stop the selected GlassFish Server instances[\\s\\S]$"));
        waitForAlertProcess("modalBody");
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertTrue(getText(prefix + "col6").endsWith(STATUS_STOPPED));
    }

    public  void gotoStandaloneInstancesPage() {
        driver.get(baseUrl + "common/index.jsf");
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
    }

}
