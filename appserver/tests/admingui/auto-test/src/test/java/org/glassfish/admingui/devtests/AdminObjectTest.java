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
 * It is used test the Connector-->admin object resource page
 * @author Jeremy Lv
 *
 */
public class AdminObjectTest extends BaseSeleniumTestClass {

    @Test
    public void testAdminObjectResources() throws Exception {
        final String resName = "adminObject" + generateRandomString();
        final String description = "Admin Object Resource - " + resName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();

        //Go to Admin Object Resources Page.
        clickAndWait("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link");

        //New Admin Object Resources
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resName);
        setFieldValue("form:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", description);
        
        int emptyCount = getTableRowCountByValue("form:basicTable", "", "col3:col1St", false);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addButton");
        
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        count = count - emptyCount;
        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue("propertyForm:resourcesTable", resName, "col1");
        assertEquals(resName, getText(prefix + "col1:link"));
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
        
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button1");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resName);
    }

    @Test
    public void testAdminObjectResourcesWithTargets() {
        final String resName = "adminObject" + generateRandomString();
        final String description = "Admin Object Resource - " + resName;
        final String instanceName = "standalone" + generateRandomString();

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        //Go to Admin Object Resources Page.
        clickAndWait("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link");

        //New Admin Object Resources
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resName);
        setFieldValue("form:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addButton");

        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");

        int emptyCount = getTableRowCountByValue("form:basicTable", "", "col3:col1St", false);
        count = count - emptyCount;

        Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select.selectByVisibleText(instanceName);
        select.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue("propertyForm:resourcesTable", resName, "col1");
        assertEquals(resName, getText(prefix + "col1:link"));
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

        // Delete admin object resource
        clickAndWait("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resName);

        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }
}
