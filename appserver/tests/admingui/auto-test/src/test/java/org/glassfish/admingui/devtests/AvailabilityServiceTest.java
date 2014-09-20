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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class AvailabilityServiceTest extends BaseSeleniumTestClass {
    public static final String ID_AVAILABILITY_SERVICE_TREE_NODE = "treeForm:tree:configurations:default-config:availabilityService:availabilityService_link";
    private static final String ID_DEFAULT_CONFIG_TURNER = "treeForm:tree:configurations:default-config:default-config_turner:default-config_turner_image";

    @Test
    public void testAvailabilityService() {
        // Expand node
        gotoDasPage();
        clickAndWait(ID_DEFAULT_CONFIG_TURNER);
        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", generateRandomString());
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", count);
        
        //Delete the property used to test after the test finished
        gotoDasPage();
        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE);
        clickByIdAction("propertyForm:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("propertyForm:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("propertyForm:basicTable:topActionsGroup1:button1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

//    //TODO:the test need to be finished after the issue of GLASSFISH-20810 had to be resolved! 
//    @Test
//    public void testWebContainerAvailability() {
//        gotoDasPage();
//        clickAndWait(ID_DEFAULT_CONFIG_TURNER);
//        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE);
//        clickAndWait("propertyForm:availabilityTabs:webAvailabilityTab");
//
//        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", generateRandomString());
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", generateRandomString());
//        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
//        assertTableRowCount("propertyForm:basicTable", count);
//    }
//
//  //TODO:the test need to be finished after the issue of GLASSFISH-20810 had to be resolved!
//    @Test
//    public void testEjbContainerAvailability() {
//        gotoDasPage();
//        clickAndWait(ID_DEFAULT_CONFIG_TURNER);
//        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE);
//        clickAndWait("propertyForm:availabilityTabs:ejbAvailabilityTab");
//
//        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", generateRandomString());
//        sleep(500);
//        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", generateRandomString());
//        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
//        assertTableRowCount("propertyForm:basicTable", count);
//        
//    }

    @Test
    public void testJMSAvailability() {
        final String clusterName = "cluster" + generateRandomString();
        final String DB_VENDOR = "mysql";
        final String DB_USER = generateRandomString();
        final String DB_URL = "jdbc:mysql://hostname:portno/dbname?password=" + generateRandomString();
        final String DB_PASSWORD = generateRandomString();

        ClusterTest ct = new ClusterTest();
        ct.createCluster(clusterName);
        
        clickAndWait("treeForm:tree:configurations:" + clusterName + "-config:availabilityService:availabilityService_link");
        clickAndWait("propertyForm:availabilityTabs:jmsAvailabilityTab");

        if (!driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:AvailabilityEnabledProp:avail")).isSelected()) {
            clickAndWait("propertyForm:propertySheet:propertSectionTextField:AvailabilityEnabledProp:avail:avail_label");
        }
        
        isElementPresent("propertyForm:propertySheet:propertSectionTextField:ConfigStoreTypeProp:ConfigStoreType");
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:ConfigStoreTypeProp:ConfigStoreType")));
        select.selectByVisibleText("masterbroker");
        isElementPresent("propertyForm:propertySheet:propertSectionTextField:MessageStoreTypeProp:MessageStoreType");
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:MessageStoreTypeProp:MessageStoreType")));
        select1.selectByVisibleText("file");
        
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbVendorProp:DbVendor", DB_VENDOR);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUserNameProp:DbUserName", DB_USER);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbPasswordProp:DbPassword", DB_PASSWORD);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUrlProp:DbUrl", DB_URL);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", generateRandomString());
        
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:" + clusterName + "-config:availabilityService:availabilityService_link");
        clickAndWait("propertyForm:availabilityTabs:jmsAvailabilityTab");

//        //The availability service in JMS Availability tap can't be enabled, seems this is a bug need to be resolved first!
//        assertTrue(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:AvailabilityEnabledProp:avail")).isSelected());

        assertEquals(DB_VENDOR, getValue("propertyForm:propertySheet:propertSectionTextField:DbVendorProp:DbVendor", "value"));
        assertEquals(DB_USER, getValue("propertyForm:propertySheet:propertSectionTextField:DbUserNameProp:DbUserName", "value"));
        assertEquals(DB_PASSWORD, getValue("propertyForm:propertySheet:propertSectionTextField:DbPasswordProp:DbPassword", "value"));
        assertEquals(DB_URL, getValue("propertyForm:propertySheet:propertSectionTextField:DbUrlProp:DbUrl", "value"));
        assertTableRowCount("propertyForm:basicTable", count);
        
        //Delete the property used to test after the test finished
        clickByIdAction("propertyForm:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("propertyForm:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("propertyForm:basicTable:topActionsGroup1:button1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        ct.deleteAllCluster();
    }
}
