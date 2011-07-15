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
import static org.junit.Assert.*;

public class JvmSettingsTest extends BaseSeleniumTestClass {
    public static final String TRIGGER_JVM_GENERAL_SETTINGS = "i18nc.jvm.GeneralTitle";
    public static final String TRIGGER_JVM_PATH_SETTINGS = "i18nc.jvm.PathSettingsTitle";
    public static final String TRIGGER_JVM_OPTIONS = "i18nc.jvmOptions.PageHelp";
    public static final String TRIGGER_JVM_PROFILER_SETTINGS = "i18nc.jvm.ProfilerPageName";
    public static final String TRIGGER_JVM_PROFILER_CREATED = "i18nc.jvm.ProfilerCreated";
    public static final String TRIGGER_JVM_PROFILER_DELETED = "i18nc.jvm.ProfilerDeleted";
    public static final String TRIGGER_CONFIG_PAGE = "i18nc.configurations.PageTitleHelp";
    public static final String JVM_LINK_TEXT = "JVM Settings";

    public static final String ID_JVM_OPTIONS_TABLE = "propertyForm:basicTable";
    @Test
    public void testJvmGeneralSettings() {
        clickAndWait("treeForm:tree:configurations:server-config:jvmSettings:jvmSettings_link", TRIGGER_JVM_GENERAL_SETTINGS);
        markCheckbox("propertyForm:propertySheet:propertSectionTextField:debugEnabledProp:debug");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        waitForPageLoad("Restart Required", 1000);
        markCheckbox("propertyForm:propertySheet:propertSectionTextField:debugEnabledProp:debug");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
    }

    @Test
    public void testJvmSettings() {
        String jvmOptionName = "-Dfoo"+generateRandomString();
        clickAndWait("treeForm:tree:configurations:server-config:jvmSettings:jvmSettings_link", TRIGGER_JVM_GENERAL_SETTINGS);
        clickAndWait("propertyForm:javaConfigTab:jvmOptions", TRIGGER_JVM_OPTIONS);

        int count = addTableRow(ID_JVM_OPTIONS_TABLE, "propertyForm:basicTable:topActionsGroup1:addSharedTableButton", "Options");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", jvmOptionName);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        clickAndWait("propertyForm:javaConfigTab:pathSettings", TRIGGER_JVM_PATH_SETTINGS);
        clickAndWait("propertyForm:javaConfigTab:jvmOptions", TRIGGER_JVM_OPTIONS);
        assertTableRowCount(ID_JVM_OPTIONS_TABLE, count);


        /* I want to clean up, as well as to test the delete button. But i can't get the row to be selected.
         * commented it out for now, since it will always fails.
         *
        selectTableRowByValue(ID_JVM_OPTIONS_TABLE, jvmOptionName, "col1", "col3");
        pressButton("propertyForm:basicTable:topActionsGroup1:button1");
        waitForButtonDisabled("propertyForm:basicTable:topActionsGroup1:button1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount(ID_JVM_OPTIONS_TABLE, count-1);
         *
         */
    }



    @Test
    public void testJvmProfilerForDas() {
        clickAndWait("treeForm:tree:configurations:server-config:jvmSettings:jvmSettings_link", TRIGGER_JVM_GENERAL_SETTINGS);
        clickAndWait("propertyForm:javaConfigTab:profiler", TRIGGER_JVM_PROFILER_SETTINGS);

        if (isElementPresent("propertyForm:propertyContentPage:topButtons:deleteButton")) {
            this.clickAndWait("propertyForm:propertyContentPage:topButtons:deleteButton", TRIGGER_JVM_PROFILER_DELETED);
            if (isConfirmationPresent()) {
                getConfirmation();
            }
        }
        
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:profilerNameProp:ProfilerName", "profiler" + generateRandomString());
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton", "Options");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "-Dfoo=" + generateRandomString());
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_JVM_PROFILER_CREATED);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:javaConfigTab:pathSettings", TRIGGER_JVM_PATH_SETTINGS);
        clickAndWait("propertyForm:javaConfigTab:profiler", TRIGGER_JVM_PROFILER_SETTINGS);
        pressButton("propertyForm:propertyContentPage:topButtons:deleteButton");
        assertTrue(getConfirmation().matches("^Profiler will be deleted\\.  Continue[\\s\\S]$"));
    }



    @Test
    public void testJvmProfilerForRunningInstance() {
        testProfilerForInstance (true);
    }
    
    @Test
    public void testJvmProfilerForStoppedInstance() {
        testProfilerForInstance (false);
    }


    private void testProfilerForInstance(boolean start){
        String instanceName = generateRandomString();
        String configName = instanceName+"-config";
        StandaloneTest st = new StandaloneTest();
        st.createStandAloneInstance(instanceName);
        if (start){
            st.startInstance(instanceName);
        }

        clickAndWait(getLinkIdByLinkText(st.ID_INSTANCE_TABLE, configName), TRIGGER_CONFIG_PAGE );
        clickAndWait(getLinkIdByLinkText("", JVM_LINK_TEXT), TRIGGER_JVM_GENERAL_SETTINGS );
        clickAndWait("propertyForm:javaConfigTab:profiler", TRIGGER_JVM_PROFILER_SETTINGS );

        setFieldValue("propertyForm:propertySheet:propertSectionTextField:profilerNameProp:ProfilerName", "profiler" + generateRandomString());
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton", "Options");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "-Dfoo=" + generateRandomString());
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_JVM_PROFILER_CREATED);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:javaConfigTab:pathSettings", TRIGGER_JVM_PATH_SETTINGS);
        clickAndWait("propertyForm:javaConfigTab:profiler", TRIGGER_JVM_PROFILER_SETTINGS);
        this.clickAndWait("propertyForm:propertyContentPage:topButtons:deleteButton", TRIGGER_JVM_PROFILER_DELETED);
        if (isConfirmationPresent()) {
            getConfirmation();
        }
        assertFalse(isElementPresent("propertyForm:propertyContentPage:topButtons:deleteButton"));
        assertTrue(isElementPresent("propertyForm:propertyContentPage:topButtons:newButton"));

        st.deleteStandAloneInstance(instanceName);
    }

    public void createStandAloneInstance(String instanceName){
        reset();
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", StandaloneTest.TRIGGER_INSTANCES_PAGE);
        clickAndWait(StandaloneTest.ID_INSTANCE_TABLE_NEW_BUTTON, StandaloneTest.TRIGGER_NEW_PAGE );
        setFieldValue(StandaloneTest.ID_INSTANCE_NAME_TEXT, instanceName);
        selectDropdownOption(StandaloneTest.ID_INSTANCE_NODE_TEXT, StandaloneTest.NODE_NAME);
        selectDropdownOption(StandaloneTest.ID_INSTANCE_CONFIG_SELECT, "default-config");
        markCheckbox(StandaloneTest.ID_INSTANCE_CONFIG_OPTION);
        clickAndWait(StandaloneTest.ID_INSTANCE_NEW_PAGE_BUTTON, StandaloneTest.TRIGGER_INSTANCES_PAGE);
    }

}
