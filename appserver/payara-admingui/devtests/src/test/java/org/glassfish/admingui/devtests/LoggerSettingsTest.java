/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

public class LoggerSettingsTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_LOGGER_SETTINGS = "i18nc.log.LoggingSettingsHelp";
    private static final String TRIGGER_LOG_LEVELS = "i18nc.log.LogLevelsPageTitleHelp";

    @Test
    public void testLoggerSettings() {
        final String rotationLimit = Integer.toString(generateRandomNumber());
        final String rotationTimeLimit = Integer.toString(generateRandomNumber());
        final String flushFrequency = Integer.toString(generateRandomNumber());

        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link", TRIGGER_LOGGER_SETTINGS);
        markCheckbox("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled");
        boolean enabled = isChecked("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled");
        setFieldValue("form1:general:sheetSection:FileRotationLimitProp:FileRotationLimit", rotationLimit);
        setFieldValue("form1:general:sheetSection:FileRotationTimeLimitProp:FileRotationTimeLimit", rotationTimeLimit);
        setFieldValue("form1:general:sheetSection:FlushFrequencyProp:FlushFrequency", flushFrequency);
        clickAndWait("form1:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("form1:loggingTabs:loggerLevels", TRIGGER_LOG_LEVELS);

        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link", TRIGGER_LOGGER_SETTINGS);
        assertEquals(enabled, isChecked("form1:general:sheetSection:writeSystemLogEnabledProp:writeSystemLogEnabled"));
        assertEquals(rotationLimit, getFieldValue("form1:general:sheetSection:FileRotationLimitProp:FileRotationLimit"));
        assertEquals(rotationTimeLimit, getFieldValue("form1:general:sheetSection:FileRotationTimeLimitProp:FileRotationTimeLimit"));
        assertEquals(flushFrequency, getFieldValue("form1:general:sheetSection:FlushFrequencyProp:FlushFrequency"));
    }

    @Test
    public void testLogLevels() {
        final String loggerName = "testLogger" + Integer.toString(generateRandomNumber());
        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link", TRIGGER_LOGGER_SETTINGS);
        clickAndWait("form1:loggingTabs:loggerLevels", TRIGGER_LOG_LEVELS);
        String newLevel = "WARNING";
        if ("WARNING".equals(getFieldValue("form1:basicTable:rowGroup1:0:col3:level"))) {
            newLevel = "INFO";
        }

        selectDropdownOption("form1:basicTable:topActionsGroup1:change_list", newLevel);
        pressButton("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");

        waitForButtonEnabled("form1:basicTable:topActionsGroup1:button2");

        pressButton("form1:basicTable:topActionsGroup1:button2");
        waitForButtonDisabled("form1:basicTable:topActionsGroup1:button2");

        clickAndWait("form1:title:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("form1:loggingTabs:loggerGeneral", TRIGGER_LOGGER_SETTINGS);
        clickAndWait("form1:loggingTabs:loggerLevels", TRIGGER_LOG_LEVELS);
        assertEquals(newLevel, getFieldValue("form1:basicTable:rowGroup1:0:col3:level"));

        clickAndWait("treeForm:tree:configurations:server-config:loggerSetting:loggerSetting_link", TRIGGER_LOGGER_SETTINGS);
        clickAndWait("form1:loggingTabs:loggerLevels", TRIGGER_LOG_LEVELS);

        // Add Logger
        int count = addTableRow("form1:basicTable", "form1:basicTable:topActionsGroup1:addSharedTableButton", "Logger Settings");
        setFieldValue("form1:basicTable:rowGroup1:0:col2:col1St", loggerName);
        clickAndWait("form1:title:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("form1:basicTable", count);

        // delete functionality through dev test framework does not select the rows and it hangs
        // need to be fixed
        //deleteRow("form1:basicTable:topActionsGroup1:button1", "form1:basicTable", loggerName);
        //clickAndWait("form1:title:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
    } 
}
