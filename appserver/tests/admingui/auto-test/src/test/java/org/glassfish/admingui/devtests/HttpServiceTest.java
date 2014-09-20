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
public class HttpServiceTest extends BaseSeleniumTestClass {
    
    @Test
    public void testHttpService() {
        gotoDasPage();
        final String interval = Integer.toString(generateRandomNumber(2880));
        final String maxFiles = Integer.toString(generateRandomNumber(50));
        final String bufferSize = Integer.toString(generateRandomNumber(65536));
        final String logWriteInterval = Integer.toString(generateRandomNumber(600));

        clickAndWait("treeForm:tree:configurations:server-config:httpService:httpService_link");
        if(!driver.findElement(By.id("form1:propertySheet:http:acLog:ssoEnabled")).isSelected())
            clickByIdAction("form1:propertySheet:http:acLog:ssoEnabled");
        
        if(!driver.findElement(By.id("form1:propertySheet:accessLog:acLog:accessLoggingEnabled")).isSelected())
            clickByIdAction("form1:propertySheet:accessLog:acLog:accessLoggingEnabled");
        
        setFieldValue("form1:propertySheet:accessLog:intervalProp:Interval", interval);
        setFieldValue("form1:propertySheet:accessLog:MaxHistoryFiles:MaxHistoryFiles", maxFiles);
        setFieldValue("form1:propertySheet:accessLog:accessLogBufferSize:accessLogBufferSize", bufferSize);
        setFieldValue("form1:propertySheet:accessLog:accessLogWriteInterval:accessLogWriteInterval", logWriteInterval);
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
        clickAndWait("treeForm:tree:configurations:server-config:httpService:httpService_link");
        assertEquals(true, driver.findElement(By.id("form1:propertySheet:http:acLog:ssoEnabled")).isSelected());
        assertEquals(interval, getValue("form1:propertySheet:accessLog:intervalProp:Interval", "value"));
        assertEquals(maxFiles, getValue("form1:propertySheet:accessLog:MaxHistoryFiles:MaxHistoryFiles", "value"));
        assertEquals(bufferSize, getValue("form1:propertySheet:accessLog:accessLogBufferSize:accessLogBufferSize", "value"));
        assertEquals(logWriteInterval, getValue("form1:propertySheet:accessLog:accessLogWriteInterval:accessLogWriteInterval", "value"));
        assertTableRowCount("form1:basicTable", count);
        
        //delete the property used to test
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }
}
