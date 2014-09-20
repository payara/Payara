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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class JdbcTest extends BaseSeleniumTestClass {

    public static final String ID_JDBC_TABLE = "propertyForm:poolTable";
    public static final String ID_RESOURCE_TABLE = "propertyForm:resourcesTable";
    
    @Test
    public void testPoolPing() {
        gotoDasPage();
        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:__TimerPool:link");
        clickAndWait("propertyForm:propertyContentPage:ping");
        assertTrue(driver.findElement(By.className("label_sun4")).getText().equals("Ping Succeeded"));
    }

    @Test
    public void testCreatingConnectionPool() {
        gotoDasPage();
        final String poolName = "jdbcPool" + generateRandomString();
        final String description = "devtest test connection pool - " + poolName;

        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:connectionPoolResources_link");
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");

        isElementPresent("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name");
        setFieldValue("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name", poolName);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:resTypeProp:resType")));
        select.selectByVisibleText("javax.sql.DataSource");
        
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:dbProp:db")));
        select1.selectByVisibleText("Derby");
        clickAndWait("propertyForm:propertyContentPage:topButtons:nextButton");

        setFieldValue("form2:sheet:generalSheet:descProp:desc", description);
        clickAndWait("form2:propertyContentPage:topButtons:finishButton");
        
        String prefix = getTableRowByValue(ID_JDBC_TABLE, poolName, "col1");
        assertEquals(poolName, getText(prefix + "col1:link"));
        assertEquals(description, getText(prefix + "col4:typeDesc"));

        gotoDasPage();
        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:connectionPoolResources_link");
        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", ID_JDBC_TABLE, poolName);
    }

    @Test
    public void testCreatingJdbcPoolWithoutDatabaseVendor() {
        gotoDasPage();
        final String poolName = "jdbcPool" + generateRandomString();
        final String description = "devtest test connection pool - " + poolName;

        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:connectionPoolResources_link");
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");

        isElementPresent("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name");
        setFieldValue("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name", poolName);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:resTypeProp:resType")));
        select.selectByVisibleText("javax.sql.ConnectionPoolDataSource");
        clickAndWait("propertyForm:propertyContentPage:topButtons:nextButton");

        setFieldValue("form2:sheet:generalSheet:descProp:desc", description);
        setFieldValue("form2:sheet:generalSheet:dsProp:datasourceField", poolName + "DataSource");
        clickAndWait("form2:propertyContentPage:topButtons:finishButton");
        
        String prefix = getTableRowByValue(ID_JDBC_TABLE, poolName, "col1");
        assertEquals(poolName, getText(prefix + "col1:link"));
        assertEquals(description, getText(prefix + "col4:typeDesc"));

        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", ID_JDBC_TABLE, poolName);
    }

    @Test
    public void testJdbcResources() {
        gotoDasPage();
        final String jndiName = "jdbcResource" + generateRandomString();
        final String description = "devtest test jdbc resource - " + jndiName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();
       
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        isElementPresent("form:propertySheet:propertSectionTextField:nameNew:name");
        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue(ID_RESOURCE_TABLE, jndiName, "col1");
        assertEquals(jndiName, getText(prefix + "col1:link"));
        assertEquals(description, getText(prefix + "col4:typeDesc"));

        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        assertTableRowCount("propertyForm:basicTable", count);

        //test disable button
        testDisableBtn(clickId);
        
        //test enable button
        testEnableBtn(clickId);
        
        //disable the jdbc resource so that it can be delete successfully
        clickByIdAction(clickId);
        testDisableBtn(clickId);
        
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", ID_RESOURCE_TABLE, jndiName);
    }

    @Test
    public void testJdbcResourcesWithTargets() {
        final String jndiName = "jdbcResource" + generateRandomString();
        final String instanceName = "standalone" + generateRandomString();
        final String description = "devtest test jdbc resource with targets- " + jndiName;

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        isElementPresent("form:propertySheet:propertSectionTextField:nameNew:name");
        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");

        Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select.selectByVisibleText(instanceName);
        select.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue(ID_RESOURCE_TABLE, jndiName, "col1");
        assertEquals(jndiName, getText(prefix + "col1:link"));
        assertEquals(description, getText(prefix + "col4:typeDesc"));
        
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //test disable button
        isElementPresent("propertyForm:resourcesTable:topActionsGroup1:newButton");
        String selectId = prefix + "col0:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:button3");
        
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button2");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:button2");

        //test manage target
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button2");
        clickByIdAction(clickId);
        clickAndWait("propertyForm:resEditTabs:targetTab");
        clickAndWait("propertyForm:targetTable:topActionsGroup1:manageTargetButton");
        Select select1 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected")));
        select1.selectByVisibleText(instanceName);
        select1.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickByIdAction("form:propertyContentPage:topButtons:saveButton");
        
        //delete jdbc resource
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", jndiName);
        
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }

    public void createJDBCResource(String jndiName, String description, String target, String targetType) {
        if (targetType.equals("standalone")) {
            StandaloneTest instanceTest = new StandaloneTest();
            instanceTest.createStandAloneInstance(target);
        } else if (targetType.equals("cluster")) {
            ClusterTest clusterTest = new ClusterTest();
            clusterTest.createCluster(target);
        }
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link");
        isElementPresent("propertyForm:resourcesTable:topActionsGroup1:newButton");
        clickByIdAction("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);

        if (targetType.equals("standalone")) {
            Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
            select.selectByVisibleText(target);
            select.selectByVisibleText("server");
            clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        } else if (targetType.equals("cluster")) {
            Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
            select.selectByVisibleText(target);
            select.selectByVisibleText("server");
            clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        }
        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue(ID_RESOURCE_TABLE, jndiName, "col1");
        assertEquals(jndiName, getText(prefix + "col1:link"));
        assertEquals(description, getText(prefix + "col4:typeDesc"));

    }

    public void deleteJDBCResource(String jndiName, String target, String targetType) {
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link");
        String prefix = getTableRowByValue(ID_RESOURCE_TABLE, jndiName, "col1");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", jndiName);
        assertFalse(isTextPresent(prefix, jndiName, ID_RESOURCE_TABLE));
        if (targetType.equals("standalone")) {
            //Delete the instance
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
            prefix = getTableRowByValue("propertyForm:instancesTable", target, "col1");
            deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", target);
            assertFalse(isTextPresent(prefix, target, "propertyForm:instancesTable"));
        } else if (targetType.equals("cluster")) {
            clickAndWait("treeForm:tree:clusterTreeNode:clusterTreeNode_link");
            prefix = getTableRowByValue("propertyForm:clustersTable", target, "col1");
            deleteRow("propertyForm:clustersTable:topActionsGroup1:button1", "propertyForm:clustersTable", target);
            assertFalse(isTextPresent(prefix, target, "propertyForm:clustersTable"));
        }
    }
    
    

    private void testDisableBtn(String clickId) {
        isElementPresent("propertyForm:propertyContentPage:topButtons:saveButton");
        clickByIdAction("propertyForm:propertySheet:propertSectionTextField:statusProp:enabled");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(!driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:statusProp:enabled")).isSelected());
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
        isElementPresent(clickId);
    }

    private void testEnableBtn(String clickId) {
        clickByIdAction(clickId);
        isElementPresent("propertyForm:propertyContentPage:topButtons:saveButton");
        clickByIdAction("propertyForm:propertySheet:propertSectionTextField:statusProp:enabled");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:statusProp:enabled")).isSelected());
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
        isElementPresent(clickId);
    }

}
