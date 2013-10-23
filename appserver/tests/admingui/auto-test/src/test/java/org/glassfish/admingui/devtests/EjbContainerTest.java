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
 * @author Jeremy Lv
 *
 */
public class EjbContainerTest extends BaseSeleniumTestClass {

    @Test
    public void testEjbSettings() {
        gotoDasPage();
        final String minSize = Integer.toString(generateRandomNumber(64));
        final String maxSize = Integer.toString(generateRandomNumber(64));
        final String poolResize = Integer.toString(generateRandomNumber(64));
        final String timeout = Integer.toString(generateRandomNumber(600));

        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");

        setFieldValue("form1:propertySheet:poolSettingSection:MinSizeProp:MinSize", minSize);
        setFieldValue("form1:propertySheet:poolSettingSection:MaxSizeProp:MaxSize", maxSize);
        setFieldValue("form1:propertySheet:poolSettingSection:PoolResizeProp:PoolResize", poolResize);
        setFieldValue("form1:propertySheet:poolSettingSection:TimeoutProp:Timeout", timeout);
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        assertEquals(minSize, getValue("form1:propertySheet:poolSettingSection:MinSizeProp:MinSize", "value"));
        assertEquals(maxSize, getValue("form1:propertySheet:poolSettingSection:MaxSizeProp:MaxSize", "value"));
        assertEquals(poolResize, getValue("form1:propertySheet:poolSettingSection:PoolResizeProp:PoolResize", "value"));
        assertEquals(timeout, getValue("form1:propertySheet:poolSettingSection:TimeoutProp:Timeout", "value"));
        assertTableRowCount("form1:basicTable", count);
        
        //delete the property used to test
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    @Test
    public void testMdbSettings() {
        gotoDasPage();
        final String minSize = Integer.toString(generateRandomNumber(64));
        final String maxSize = Integer.toString(generateRandomNumber(64));
        final String poolResize = Integer.toString(generateRandomNumber(64));
        final String timeout = Integer.toString(generateRandomNumber(600));

        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        clickAndWait("form1:ejbContainerTabs:mdbSettingsTab");

        setFieldValue("form1:propertySheet:propertySectionTextField:MinSizeProp:MinSize", minSize);
        setFieldValue("form1:propertySheet:propertySectionTextField:MaxSizeProp:MaxSize", maxSize);
        setFieldValue("form1:propertySheet:propertySectionTextField:PoolResizeProp:PoolResize", poolResize);
        setFieldValue("form1:propertySheet:propertySectionTextField:TimeoutProp:Timeout", timeout);
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton");

        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        clickAndWait("form1:ejbContainerTabs:mdbSettingsTab");

        assertEquals(minSize, getValue("form1:propertySheet:propertySectionTextField:MinSizeProp:MinSize", "value"));
        assertEquals(maxSize, getValue("form1:propertySheet:propertySectionTextField:MaxSizeProp:MaxSize", "value"));
        assertEquals(poolResize, getValue("form1:propertySheet:propertySectionTextField:PoolResizeProp:PoolResize", "value"));
        assertEquals(timeout, getValue("form1:propertySheet:propertySectionTextField:TimeoutProp:Timeout", "value"));
        assertTableRowCount("form1:basicTable", count);
        
        //delete the property used to test
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    @Test
    public void testEjbTimerService() {
        gotoDasPage();
        final String minDelivery = Integer.toString(generateRandomNumber(5000));
        final String maxRedelivery = Integer.toString(generateRandomNumber(10));
        final String redeliveryInterval = Integer.toString(generateRandomNumber(20000));
        final String timerDatasource = "jndi/" + generateRandomString();

        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        clickAndWait("form1:ejbContainerTabs:ejbTimerTab");

        setFieldValue("form1:propertySheet:propertySectionTextField:MinDeliveryProp:MinDelivery", minDelivery);
        setFieldValue("form1:propertySheet:propertySectionTextField:MaxRedeliveryProp:MaxRedelivery", maxRedelivery);
        setFieldValue("form1:propertySheet:propertySectionTextField:RedeliveryIntrProp:RedeliveryIntr", redeliveryInterval);
        setFieldValue("form1:propertySheet:propertySectionTextField:TimerDatasourceProp:TimerDatasource", timerDatasource);
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton");

        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        clickAndWait("form1:ejbContainerTabs:ejbTimerTab");

        assertEquals(minDelivery, getValue("form1:propertySheet:propertySectionTextField:MinDeliveryProp:MinDelivery", "value"));
        assertEquals(maxRedelivery, getValue("form1:propertySheet:propertySectionTextField:MaxRedeliveryProp:MaxRedelivery", "value"));
        assertEquals(redeliveryInterval, getValue("form1:propertySheet:propertySectionTextField:RedeliveryIntrProp:RedeliveryIntr", "value"));
        assertEquals(timerDatasource, getValue("form1:propertySheet:propertySectionTextField:TimerDatasourceProp:TimerDatasource", "value"));
        assertTableRowCount("form1:basicTable", count);

        // Clean up after ourselves, just because... :)
        setFieldValue("form1:propertySheet:propertySectionTextField:MinDeliveryProp:MinDelivery", "1000");
        setFieldValue("form1:propertySheet:propertySectionTextField:MaxRedeliveryProp:MaxRedelivery", "1");
        setFieldValue("form1:propertySheet:propertySectionTextField:RedeliveryIntrProp:RedeliveryIntr", "5000");
        setFieldValue("form1:propertySheet:propertySectionTextField:TimerDatasourceProp:TimerDatasource", "");
        
        //delete the property used to test
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    //Test that the default button in EJB Settings will fill in the default value when pressed.
    @Test
    public void testEjbSettingsDefault() {
        gotoDasPage();
        //Go to EJB Settings page, enter some random value
        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");
        clickByIdAction("form1:propertySheet:generalPropertySection:commitOptionProp:optC");
        setFieldValue("form1:propertySheet:poolSettingSection:MinSizeProp:MinSize", "2");
        setFieldValue("form1:propertySheet:poolSettingSection:MaxSizeProp:MaxSize", "34");
        setFieldValue("form1:propertySheet:poolSettingSection:PoolResizeProp:PoolResize", "10");
        setFieldValue("form1:propertySheet:poolSettingSection:TimeoutProp:Timeout", "666");
        setFieldValue("form1:propertySheet:cacheSettingSection:MaxCacheProp:MaxCache", "520");
        setFieldValue("form1:propertySheet:cacheSettingSection:CacheResizeProp:CacheResize", "36");
        setFieldValue("form1:propertySheet:cacheSettingSection:RemTimoutProp:RemTimout", "5454");
        Select select1 = new Select(driver.findElement(By.id("form1:propertySheet:cacheSettingSection:RemPolicyProp:RemPolicy")));
        select1.selectByVisibleText("First In First Out (fifo)");
        setFieldValue("form1:propertySheet:cacheSettingSection:CacheIdleProp:CacheIdle", "666");

        //Save this, goto another tab and back
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:ejbContainer:ejbContainer_link");

        //Now, click the default button and ensure all the default values are filled in
        //The default value should match whats specified in the config bean.
        //Save and come back to the page to assert.

        //Location should not be changed by the default button
        String location = getValue("form1:propertySheet:generalPropertySection:SessionStoreProp:SessionStore", "value");

        //We are testing that default button fills in the correct default value, not testing if the Save button works.
        //no need to click Save for this test.
        clickAndWait("form1:propertyContentPage:loadDefaultsButton");
        waitForButtonEnabled("form1:propertyContentPage:loadDefaultsButton");

        assertEquals(location, getValue("form1:propertySheet:generalPropertySection:SessionStoreProp:SessionStore", "value"));
        
        assertEquals(true, driver.findElement(By.id(("form1:propertySheet:generalPropertySection:commitOptionProp:optB"))).isSelected());
        assertEquals("0", getValue("form1:propertySheet:poolSettingSection:MinSizeProp:MinSize", "value"));
        assertEquals("32", getValue("form1:propertySheet:poolSettingSection:MaxSizeProp:MaxSize", "value"));
        assertEquals("8", getValue("form1:propertySheet:poolSettingSection:PoolResizeProp:PoolResize", "value"));
        assertEquals("600", getValue("form1:propertySheet:poolSettingSection:TimeoutProp:Timeout", "value"));
        assertEquals("512", getValue("form1:propertySheet:cacheSettingSection:MaxCacheProp:MaxCache", "value"));
        assertEquals("32", getValue("form1:propertySheet:cacheSettingSection:CacheResizeProp:CacheResize", "value"));
        assertEquals("5400", getValue("form1:propertySheet:cacheSettingSection:RemTimoutProp:RemTimout", "value"));
        assertEquals("nru", getValue("form1:propertySheet:cacheSettingSection:RemPolicyProp:RemPolicy", "value"));
        assertEquals("600", getValue("form1:propertySheet:cacheSettingSection:CacheIdleProp:CacheIdle", "value"));

        //will be nice to have the default value back for the server.
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
    }
}
