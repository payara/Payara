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
public class IiopListenerTest extends BaseSeleniumTestClass {

    @Test
    public void testAddIiopListener() {
        gotoDasPage();
        final String iiopName = "testIiopListener" + generateRandomString();
        final String networkAddress = "0.0.0.0";
        final String listenerPort = Integer.toString(generateRandomNumber(32768));;
        final String certName = "s1as";

        clickAndWait("treeForm:tree:configurations:server-config:orb:iiopListeners:iiopListeners_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:IiopNameTextProp:IiopNameText", iiopName);
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr", networkAddress);
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort", listenerPort);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "a");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:configs", iiopName, "col1");
        assertEquals(iiopName, getText(prefix + "col1:link"));
        
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        assertEquals(networkAddress, getValue("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr", "value"));
        assertEquals(listenerPort, getValue("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort", "value"));

        assertTableRowCount("propertyForm:basicTable", count);

        // access the SSL Page
        clickAndWait("propertyForm:iiopTab:sslEdit");
        setFieldValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname", certName);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        assertEquals(certName, getValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname", "value"));

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", iiopName);
    }

    @Test
    public void testORB() {
        gotoDasPage();
        final String totalConn = "1048";
        final String maxMsgSize = "2048";
        clickAndWait("treeForm:tree:configurations:server-config:orb:orb_link");
        setFieldValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns", totalConn);
        Select select = new Select(driver.findElement(By.id("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize")));
        select.selectByVisibleText(maxMsgSize);
        
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        assertEquals(totalConn, getValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns", "value"));
        assertEquals(maxMsgSize, getValue("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize", "value"));
    }
}
