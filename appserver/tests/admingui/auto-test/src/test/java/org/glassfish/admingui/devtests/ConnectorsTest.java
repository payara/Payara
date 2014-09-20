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
public class ConnectorsTest extends BaseSeleniumTestClass {

    @Test
    public void testConnectorResources() {
        String testPool = "connectorPool" + generateRandomString();
        String testConnector = "connectorResource" + generateRandomString();

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();

        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");

        // Create new connection connection pool
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");

        setFieldValue("propertyForm:propertySheet:generalPropertySheet:jndiProp:name", testPool);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:resAdapterProp:db")));
        select.selectByVisibleText("jmsra");
        sleep(1000);
        
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:connectionDefProp:db")));
        select1.selectByVisibleText("javax.jms.QueueConnectionFactory");
        waitForButtonEnabled("propertyForm:title:topButtons:nextButton");

        clickAndWait("propertyForm:title:topButtons:nextButton");

        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertySheet:poolPropertySheet:transprop:trans")));
        select2.selectByVisibleText("NoTransaction");
        clickAndWait("propertyForm:propertyContentPage:topButtons:finishButton");
        
        String prefix = getTableRowByValue("propertyForm:poolTable", testPool, "col1");
        assertEquals(testPool, getText(prefix + "col1:link"));
        
        // Create new connector resource which uses this new pool
        clickAndWait("treeForm:tree:resources:Connectors:connectorResources:connectorResources_link");

        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:propertSectionTextField:jndiTextProp:jnditext", testConnector);
        Select select3 = new Select(driver.findElement(By.id("form:propertySheet:propertSectionTextField:poolNameProp:PoolName")));
        select3.selectByVisibleText(testPool);

        clickAndWait("form:propertyContentPage:topButtons:newButton");

        
        //test disable button
        String connectorPrefix = getTableRowByValue("propertyForm:resourcesTable", testConnector, "col1");
        isElementPresent("propertyForm:resourcesTable:topActionsGroup1:newButton");
        String selectId = connectorPrefix + "col0:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:button3");
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button2");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:button2");
        
        // Delete connector resource
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button1");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", testConnector);

        // Delete connector connection pool
        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");

        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", testPool);
    }


    @Test
    public void testConnectorResourcesWithTargets() {
        gotoDasPage();
        String testPool = "connectorPool" + generateRandomString();
        String testConnector = "connectorResource" + generateRandomString();
        final String instanceName = "standalone" + generateRandomString();

        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");

        // Create new connection connection pool
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");

        setFieldValue("propertyForm:propertySheet:generalPropertySheet:jndiProp:name", testPool);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:resAdapterProp:db")));
        select.selectByVisibleText("jmsra");
        sleep(1000);
        
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:connectionDefProp:db")));
        select1.selectByVisibleText("javax.jms.QueueConnectionFactory");
        waitForButtonEnabled("propertyForm:title:topButtons:nextButton");

        clickAndWait("propertyForm:title:topButtons:nextButton");

        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertySheet:poolPropertySheet:transprop:trans")));
        select2.selectByVisibleText("NoTransaction");
        clickAndWait("propertyForm:propertyContentPage:topButtons:finishButton");
        
        String prefix = getTableRowByValue("propertyForm:poolTable", testPool, "col1");
        assertEquals(testPool, getText(prefix + "col1:link"));

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        // Create new connector resource which uses this new pool
        clickAndWait("treeForm:tree:resources:Connectors:connectorResources:connectorResources_link");

        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:propertSectionTextField:jndiTextProp:jnditext", testConnector);
        Select select3 = new Select(driver.findElement(By.id("form:propertySheet:propertSectionTextField:poolNameProp:PoolName")));
        select3.selectByVisibleText(testPool);
        
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");

        Select select4 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select4.selectByVisibleText(instanceName);
        select4.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String connectorPrefix = getTableRowByValue("propertyForm:resourcesTable", testConnector, "col1");
        assertEquals(testConnector, getText(connectorPrefix + "col1:link"));
        
        String clickId = connectorPrefix + "col1:link";
        clickByIdAction(clickId);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //test disable button
        isElementPresent("propertyForm:resourcesTable:topActionsGroup1:newButton");
        String selectId = connectorPrefix + "col0:select";
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
        Select select5 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected")));
        select5.selectByVisibleText(instanceName);
        select5.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickByIdAction("form:propertyContentPage:topButtons:saveButton");
        

        // Delete connector resource
        clickAndWait("treeForm:tree:resources:Connectors:connectorResources:connectorResources_link");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", testConnector);

        // Delete connector connection pool
        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");
        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", testPool);

        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }

    
//    //This tests need to be finished and retested after the GLASSFISH-20812 has been resolved!
//    @Test
//    public void testConnectorSecurityMaps() {
//        gotoDasPage();
//        String testPool = generateRandomString();
//        String testSecurityMap = generateRandomString();
//        String testGroup = generateRandomString();
//        String testPassword = generateRandomString();
//        String testUserName = generateRandomString();
//
//        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");
//
//        // Create new connection connection pool
//        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");
//
//        setFieldValue("propertyForm:propertySheet:generalPropertySheet:jndiProp:name", testPool);
//        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:resAdapterProp:db")));
//        select.selectByVisibleText("jmsra");
//        sleep(1000);
//        
//        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:generalPropertySheet:connectionDefProp:db")));
//        select1.selectByVisibleText("javax.jms.QueueConnectionFactory");
//        waitForButtonEnabled("propertyForm:title:topButtons:nextButton");
//
//        clickAndWait("propertyForm:title:topButtons:nextButton");
//
//        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertySheet:poolPropertySheet:transprop:trans")));
//        select2.selectByVisibleText("NoTransaction");
//        clickAndWait("propertyForm:propertyContentPage:topButtons:finishButton");
//        
//        String prefix = getTableRowByValue("propertyForm:poolTable", testPool, "col1");
//        assertEquals(testPool, getText(prefix + "col1:link"));
//
//        //Create Connector Security Map
//        String clickId = prefix + "col1:link";
//        clickByIdAction(clickId);
//        clickAndWait("propertyForm:connectorPoolSet:securityMapTab");
//        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");
//
//        setFieldValue("propertyForm:propertySheet:propertSectionTextField:mapNameNew:mapName", testSecurityMap);
//        setFieldValue("propertyForm:propertySheet:propertSectionTextField:groupProp:group", testGroup);
//        setFieldValue("propertyForm:propertySheet:propertSectionTextField2:userNameEdit:userNameEdit", testUserName);
//        setFieldValue("propertyForm:propertySheet:propertSectionTextField2:passwordEdit:passwordEdit", testPassword);
//        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
//
//        String securityPrefix = getTableRowByValue("propertyForm:resourcesTable", testSecurityMap, "col1");
//        String clickId1 = securityPrefix + "col1:link";
//        clickByIdAction(clickId1);
//        Assert.assertEquals(testGroup, getValue("propertyForm:propertySheet:propertSectionTextField:groupProp:group", "value"));
//        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
//
//        //Delete Connector Security Maps
//        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", testSecurityMap);
//
//        // Delete connector connection pool
//        clickAndWait("treeForm:tree:resources:Connectors:connectorConnectionPools:connectorConnectionPools_link");
//        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", testPool);        
//    }
}
