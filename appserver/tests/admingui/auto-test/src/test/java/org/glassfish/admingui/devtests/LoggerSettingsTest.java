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
public class LoggerSettingsTest extends BaseSeleniumTestClass {

    @Test
    public void testLoggerSettings() {
        gotoDasPage();
        final String rotationLimit = Integer.toString(generateRandomNumber());
        final String rotationTimeLimit = Integer.toString(generateRandomNumber());
        final String flushFrequency = Integer.toString(generateRandomNumber());

        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        if (!driver.findElement(By.id("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled")).isSelected()){
            clickByIdAction("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled");
        }
        setFieldValue("form1:general:sheetSection:FileRotationLimitProp:FileRotationLimit", rotationLimit);
        setFieldValue("form1:general:sheetSection:FileRotationTimeLimitProp:FileRotationTimeLimit", rotationTimeLimit);
        setFieldValue("form1:general:sheetSection:FlushFrequencyProp:FlushFrequency", flushFrequency);
        clickAndWait("form1:propertyContentPage:topButtons:saveButton");

        clickAndWait("form1:loggingTabs:loggerLevels");

        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        assertTrue(driver.findElement(By.id("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled")).isSelected());
        assertEquals(rotationLimit, getValue("form1:general:sheetSection:FileRotationLimitProp:FileRotationLimit", "value"));
        assertEquals(rotationTimeLimit, getValue("form1:general:sheetSection:FileRotationTimeLimitProp:FileRotationTimeLimit", "value"));
        assertEquals(flushFrequency, getValue("form1:general:sheetSection:FlushFrequencyProp:FlushFrequency", "value"));
    }

    @Test
    public void testLogLevels() {
        gotoDasPage();
        final String loggerName = "testLogger" + Integer.toString(generateRandomNumber());
        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        clickAndWait("form1:loggingTabs:loggerLevels");
        String newLevel = "WARNING";
        if ("WARNING".equals(getValue("form1:basicTable:rowGroup1:0:col3:level", "value"))) {
            newLevel = "INFO";
        }

        Select select = new Select(driver.findElement(By.id("form1:basicTable:topActionsGroup1:change_list")));
        select.selectByVisibleText(newLevel);
        clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");

        waitForButtonEnabled("form1:basicTable:topActionsGroup1:button2");

        clickByIdAction("form1:basicTable:topActionsGroup1:button2");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button2");
        clickAndWait("form1:title:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        clickAndWait("form1:loggingTabs:loggerLevels");
        assertEquals(newLevel, getValue("form1:basicTable:rowGroup1:0:col3:level", "value"));

        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        clickAndWait("form1:loggingTabs:loggerLevels");
        // Add Logger
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton", "Logger Settings");
        sleep(500);
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", loggerName);
        clickAndWait("form1:title:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("form1:basicTable", count);
        
        //delete the property used to test
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link");
        clickAndWait("form1:loggingTabs:loggerLevels");
        String prefix = getTableRowByVal("form1:basicTable", loggerName, "col2:col1St");
        String selectId = prefix + "col1:select";
        clickByIdAction(selectId);
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:title:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    } 
}
