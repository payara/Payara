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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 
 * @author Jeremy Lv
 *
 */
public class ApplicationTest extends BaseSeleniumTestClass {

    private static final String ELEMENT_STATUS = "propertyForm:propertySheet:propertSectionTextField:statusProp:status";
    private static final String ELEMENT_APP_NAME = "form:war:psection:nameProp:appName";
    private static final String ELEMENT_CONTEXT_ROOT = "form:war:psection:cxp:ctx";
    private static final String ELEMENT_UNDEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:button1";
    private static final String ELEMENT_DEPLOY_TABLE = "propertyForm:deployTable";
    private static final String ELEMENT_ENABLE_BUTTON = "propertyForm:deployTable:topActionsGroup1:button2";
    private static final String ELEMENT_CANCEL_BUTTON = "propertyForm:propertyContentPage:topButtons:cancelButton";
    private static final String ELEMENT_DISABLE_BUTTON = "propertyForm:deployTable:topActionsGroup1:button3";
    private static final String ELEMENT_UPLOAD_BUTTON = "form:title:topButtons:uploadButton";
    private static final String ELEMENT_FILE_FIELD = "form:sheet1:section1:prop1:fileupload";

    //The following test will pass ONLY if there is no cluster or standalone instance.  This is for "PE" profile
    @Test
    public void testDeployWar() {
        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllCluster();
        final String applicationName = generateRandomString();
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
        File war = new File("src/test/resources/test.war");
        driver.findElement(By.id(ELEMENT_FILE_FIELD)).sendKeys(war.getAbsoluteFile().toString());
        
        assertEquals("test", getValue(ELEMENT_CONTEXT_ROOT, "value"));
        assertEquals("test", getValue(ELEMENT_APP_NAME, "value"));

        setFieldValue(ELEMENT_CONTEXT_ROOT, applicationName);
        setFieldValue(ELEMENT_APP_NAME, applicationName);
        clickAndWait(ELEMENT_UPLOAD_BUTTON);

        //add some sleep time here to wait for the webdriver element located
        sleep(10000);
        gotoDasPage();
        clickAndWait("treeForm:tree:applications:applications_link");
        sleep(1000);
        int postCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount < postCount);
        
        // Disable application
        String prefix = getTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName, "col1");
        assertEquals(applicationName, getText(prefix + "col1:link"));
        String selectId = prefix + "col0:select";
        clickByIdAction(selectId);
        clickAndWait(ELEMENT_DISABLE_BUTTON);
        waitforBtnDisable(ELEMENT_DISABLE_BUTTON);
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        assertFalse(driver.findElement(By.id(ELEMENT_STATUS)).isSelected());
        clickAndWait(ELEMENT_CANCEL_BUTTON);

        // Enable Application
        clickByIdAction(selectId);
        clickAndWait(ELEMENT_ENABLE_BUTTON);
        waitforBtnDisable(ELEMENT_ENABLE_BUTTON);
        clickByIdAction(clickId);
        assertTrue(driver.findElement(By.id(ELEMENT_STATUS)).isSelected());
        clickAndWait(ELEMENT_CANCEL_BUTTON);
        
        // Undeploy application
        clickByIdAction(selectId);
        clickAndWait(ELEMENT_UNDEPLOY_BUTTON);
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        waitforBtnDisable(ELEMENT_UNDEPLOY_BUTTON);
        sleep(1000);
        int postUndeployCount = getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount == postUndeployCount);
    }

    @Test
    public void testApplicationConfiguration() {
        gotoDasPage();
        final String adminTimeout = Integer.toString(generateRandomNumber(100));
        clickAndWait("treeForm:tree:nodes:nodes_link");
        clickAndWait("propertyForm:domainTabs:appConfig");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout", adminTimeout);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertEquals(adminTimeout, getValue("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout", "value"));
    }

    @Test
    public void testDomainAttributes() {
        gotoDasPage();
        clickAndWait("treeForm:tree:nodes:nodes_link");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "en_UK");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertEquals("en_UK", getValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "value"));
    }

    @Test
    public void testDomainLogs() {
        gotoDasPage();
        clickAndWait("treeForm:tree:nodes:nodes_link");
        clickAndWait("propertyForm:domainTabs:domainLogs");
        // click download, but ignore it (selenium can't interect with Save File dialog
        String winHandleBefore = driver.getWindowHandle();
        clickByIdAction("form:propertyContentPage:topButtons:collectLogFiles");
        for(String winHandle : driver.getWindowHandles()){
            driver.switchTo().window(winHandle);
        }
        driver.close();
        driver.switchTo().window(winHandleBefore);
        // if above is broken, assertion will fail
        assertEquals("Domain Logs", driver.findElement(By.className("TtlTxt_sun4")).getText());
    }
}
