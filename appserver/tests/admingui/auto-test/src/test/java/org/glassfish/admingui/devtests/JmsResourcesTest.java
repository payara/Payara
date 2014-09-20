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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

/**
 * 
 * @author Jeremy Lv
 *
 */
public class JmsResourcesTest extends BaseSeleniumTestClass {

    @Test
    public void testAddingConnectionFactories() throws Exception {
        final String poolName = "JMSConnFactory" + generateRandomString();
        final String description = "Test Pool - " + poolName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();

        clickAndWait("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:generalPropertySheet:jndiProp:jndiProp", poolName);
        Select select = new Select(driver.findElement(By.id("form:propertySheet:generalPropertySheet:resTyped:resType")));
        select.selectByVisibleText("javax.jms.TopicConnectionFactory");
        setFieldValue("form:propertySheet:generalPropertySheet:descProp:descProp", description);
        Select select1 = new Select(driver.findElement(By.id("form:propertySheet:poolPropertySheet:transprop:trans")));
        select1.selectByVisibleText("LocalTransaction");
        clickAndWait("form:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", poolName, "colName");
        assertEquals(poolName, getText(prefix + "colName:link"));

        //test disable button
        String selectId = prefix + "colSelect:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:deleteConnButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:deleteConnButton");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
    }

    @Test
    public void testAddingConnectionFactoriesWithTargets() throws Exception {
        final String poolName = "JMSConnFactory" + generateRandomString();
        final String description = "Test Pool - " + poolName;
        final String instanceName = "standalone" + generateRandomString();
        
        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:generalPropertySheet:jndiProp:jndiProp", poolName);
        Select select = new Select(driver.findElement(By.id("form:propertySheet:generalPropertySheet:resTyped:resType")));
        select.selectByVisibleText("javax.jms.TopicConnectionFactory");
        setFieldValue("form:propertySheet:generalPropertySheet:descProp:descProp", description);
        Select select1 = new Select(driver.findElement(By.id("form:propertySheet:poolPropertySheet:transprop:trans")));
        select1.selectByVisibleText("LocalTransaction");
        
        
        Select select2 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select2.selectByVisibleText(instanceName);
        select2.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        clickAndWait("form:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", poolName, "colName");
        assertEquals(poolName, getText(prefix + "colName:link"));

        //test disable button
        String selectId = prefix + "colSelect:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        String clickId = prefix + "colName:link";
        clickByIdAction(clickId);
        clickAndWait("propertyForm:resEditTabs:targetTab");
        clickAndWait("propertyForm:targetTable:topActionsGroup1:manageTargetButton");
        Select select5 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected")));
        select5.selectByVisibleText(instanceName);
        select5.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickByIdAction("form:propertyContentPage:topButtons:saveButton");
        

        gotoDasPage();
        clickAndWait("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:deleteConnButton");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }

    @Test
    public void testAddingDestinationResources() throws Exception {
        final String resourceName = "JMSDestination" + generateRandomString();
        final String description = "Test Destination - " + resourceName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();

        clickAndWait("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link");
        sleep(1000);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:jndiProp:jndi", resourceName);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:nameProp:name", "somePhysicalDestination");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:descProp:desc", description);
        clickAndWait("form:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", resourceName, "colName");
        assertEquals(resourceName, getText(prefix + "colName:link"));
        

        //test disable button
        String selectId = prefix + "colSelect:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:deleteDestButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:deleteDestButton");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
    }

    @Test
    public void testAddingDestinationResourcesWithTargets() throws Exception {
        final String resourceName = "JMSDestination" + generateRandomString();
        final String instanceName = "standalone" + generateRandomString();
        final String description = "Test Destination - " + resourceName;

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link");
        sleep(1000);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:jndiProp:jndi", resourceName);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:nameProp:name", "somePhysicalDestination");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:descProp:desc", description);

        Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select.selectByVisibleText(instanceName);
        select.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", resourceName, "colName");
        assertEquals(resourceName, getText(prefix + "colName:link"));

        //test disable button
        String selectId = prefix + "colSelect:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        
        //test enable button
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        String clickId = prefix + "colName:link";
        clickByIdAction(clickId);
        clickAndWait("propertyForm:resEditTabs:targetTab");
        clickAndWait("propertyForm:targetTable:topActionsGroup1:manageTargetButton");
        Select select5 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected")));
        select5.selectByVisibleText(instanceName);
        select5.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickByIdAction("form:propertyContentPage:topButtons:saveButton");
        
        gotoDasPage();
        clickAndWait("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link");
        clickByIdAction(selectId);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:deleteDestButton");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }

/*
    @Test
    public void testAddingTransport() {

    }
*/
}
