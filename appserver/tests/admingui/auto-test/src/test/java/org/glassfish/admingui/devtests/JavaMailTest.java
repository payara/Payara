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
public class JavaMailTest extends BaseSeleniumTestClass {

    @Test
    public void createMailResource() {
        gotoDasPage();
        final String resourceName = "javaMail" + generateRandomString();
        final String description = resourceName + " description";

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();
        
        clickAndWait("treeForm:tree:resources:mailResources:mailResources_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        isElementPresent("form:propertySheet:propertSectionTextField:nameNew:name");
        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resourceName);
        setFieldValue("form:propertySheet:propertSectionTextField:hostProp:host", "localhost");
        setFieldValue("form:propertySheet:propertSectionTextField:userProp:user", "user");
        setFieldValue("form:propertySheet:propertSectionTextField:fromProp:from", "return@test.com");
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", resourceName, "col1");
        assertEquals(resourceName, getText(prefix + "col1:link"));

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
        
        waitforBtnDisable("propertyForm:resourcesTable:topActionsGroup1:button2");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resourceName);
    }

    @Test
    public void createMailResourceWithTargets() {
        final String resourceName = "javaMail" + generateRandomString();
        final String description = resourceName + " description";
        final String instanceName = "standalone" + generateRandomString();

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:mailResources:mailResources_link");
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton");

        isElementPresent("form:propertySheet:propertSectionTextField:nameNew:name");
        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resourceName);
        setFieldValue("form:propertySheet:propertSectionTextField:hostProp:host", "localhost");
        setFieldValue("form:propertySheet:propertSectionTextField:userProp:user", "user");
        setFieldValue("form:propertySheet:propertSectionTextField:fromProp:from", "return@test.com");
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");

        Select select = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available")));
        select.selectByVisibleText(instanceName);
        select.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton");

        
        String prefix = getTableRowByValue("propertyForm:resourcesTable", resourceName, "col1");
        assertEquals(resourceName, getText(prefix + "col1:link"));

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
        Select select5 = new Select(driver.findElement(By.id("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected")));
        select5.selectByVisibleText(instanceName);
        select5.selectByVisibleText("server");
        clickByIdAction("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickByIdAction("form:propertyContentPage:topButtons:saveButton");

        
        gotoDasPage();
        clickAndWait("treeForm:tree:resources:mailResources:mailResources_link");
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resourceName);
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }
}
