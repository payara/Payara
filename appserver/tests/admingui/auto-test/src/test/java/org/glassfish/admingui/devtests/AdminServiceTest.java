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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class AdminServiceTest extends BaseSeleniumTestClass {

    @Test
    public void testEditJmxConntector() {
        gotoDasPage();
        String address = generateRandomNumber(255)+"."+generateRandomNumber(255)+"."+generateRandomNumber(255)+"."+generateRandomNumber(255);
        clickAndWait("treeForm:tree:configurations:server-config:adminService:adminService_link");
        if (!driver.findElement(By.id("form1:propertySheet:propertySheetSection:SecurityProp:Security")).isSelected())
            clickByIdAction("form1:propertySheet:propertySheetSection:SecurityProp:Security");
        setFieldValue("form1:propertySheet:propertySheetSection:AddressProp:Address", address);
        int count = addTableRow("form1:basicTable","form1:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form1:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form1:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:adminService:adminService_link");
        assertTrue(driver.findElement(By.id("form1:propertySheet:propertySheetSection:SecurityProp:Security")).isSelected());
        assertEquals(address, getValue("form1:propertySheet:propertySheetSection:AddressProp:Address", "value"));
        assertTableRowCount("form1:basicTable", count);
        
        //delete the property used to test
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:adminService:adminService_link");
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    
    @Test
    public void testSsl() {
        gotoDasPage();
        final String nickname = "nickname"+generateRandomString();
        final String keystore = "keystore"+generateRandomString()+".jks";
        final String maxCertLength = Integer.toString(generateRandomNumber(10));

        clickAndWait("treeForm:tree:configurations:server-config:adminService:adminService_link");
        clickAndWait("form1:jmxConnectorTab:jmxSSLEdit");

        waitForElementPresent("TtlTxt_sun4", "SSL");
        if(driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:SSL3Prop:SSL3")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:propertySheetSection:SSL3Prop:SSL3");
        }
        if(driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:TLSProp:TLS")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:propertySheetSection:TLSProp:TLS");
        }
        if(!driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:ClientAuthProp:ClientAuth")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:propertySheetSection:ClientAuthProp:ClientAuth");
        }
        setFieldValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname", nickname);
        setFieldValue("propertyForm:propertySheet:propertySheetSection:keystore:keystore", keystore);
        setFieldValue("propertyForm:propertySheet:propertySheetSection:maxCertLength:maxCertLength", maxCertLength);
//        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:adminService:adminService_link");
        clickAndWait("form1:jmxConnectorTab:jmxSSLEdit");

        assertEquals(false, driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:SSL3Prop:SSL3")).isSelected());
        assertEquals(false, driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:TLSProp:TLS")).isSelected());
        assertEquals(true, driver.findElement(By.id("propertyForm:propertySheet:propertySheetSection:ClientAuthProp:ClientAuth")).isSelected());
        assertEquals(nickname, getValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname", "value"));
        assertEquals(keystore, getValue("propertyForm:propertySheet:propertySheetSection:keystore:keystore", "value"));
        assertEquals(maxCertLength, getValue("propertyForm:propertySheet:propertySheetSection:maxCertLength:maxCertLength", "value"));
    }
    
}
