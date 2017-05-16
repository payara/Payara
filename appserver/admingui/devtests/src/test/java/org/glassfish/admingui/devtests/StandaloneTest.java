/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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


import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author anilam
 */
public class StandaloneTest  extends BaseSeleniumTestClass {
    public static final String TRIGGER_INSTANCES_PAGE = "i18ncs.standaloneInstances.PageTitleHelp";
    public static final String TRIGGER_NEW_PAGE = "i18ncs.clusterNew.Configuration";
    public static final String TRIGGER_GENERAL_INFO_PAGE = "i18n.instance.GeneralTitle";
    public static final String TRIGGER_SYS_PROPS = "i18nc.instanceProperties.SystemPropertiesTitle";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_STOPPED = "Stopped";
    public static final String TRIGGER_INSTANCE_TABLE = "i18ncs.standaloneInstances.TableTitle" ;
    public static final String TRIGGER_PROPS_TABLE = "i18n.common.AdditionalProperties";

    public static final String ID_INSTANCE_TABLE_NEW_BUTTON = "propertyForm:instancesTable:topActionsGroup1:newButton";
    public static final String ID_INSTANCE_TABLE_DELETE_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button1";
    public static final String ID_INSTANCE_TABLE_START_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button2";
    public static final String ID_INSTANCE_TABLE_STOP_BUTTON = "propertyForm:instancesTable:topActionsGroup1:button3";
    public static final String ID_INSTANCE_TABLE = "propertyForm:instancesTable";
    public static final String ID_INSTANCE_PROP_TAB = "propertyForm:standaloneInstanceTabs:standaloneProp";

    public static final String ID_INSTANCE_NAME_TEXT = "propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText";
    public static final String ID_INSTANCE_NODE_TEXT = "propertyForm:propertySheet:propertSectionTextField:node:node" ;
    public static final String ID_INSTANCE_CONFIG_SELECT = "propertyForm:propertySheet:propertSectionTextField:configProp:Config" ;
    public static final String ID_INSTANCE_CONFIG_OPTION = "propertyForm:propertySheet:propertSectionTextField:configOptionProp:optC";
    public static final String ID_INSTANCE_NEW_PAGE_BUTTON = "propertyForm:propertyContentPage:topButtons:newButton" ;

    public static final String INSTANCE_PREFIX = "standAlone" ;
    public static final String NODE_NAME = "localhost-domain1" ;
    public static final String DEFAULT_WEIGHT = "100" ;

    @BeforeClass
    public static void beforeClass() {
        (new StandaloneTest()).deleteAllStandaloneInstances();
    }

    @Test
    public void testCreateStartStopAndDeleteStandaloneInstance() {
        String instanceName = INSTANCE_PREFIX + generateRandomString();
        createStandAloneInstance(instanceName);

        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertTrue(isTextPresent(instanceName));
        assertEquals(instanceName, getText(prefix + "col1:link"));
        assertEquals(instanceName+"-config", getText(prefix + "col3:configlink"));
        assertEquals(NODE_NAME, getText(prefix + "col5:nodeAgentlink"));
        assertEquals(STATUS_STOPPED, getText(prefix + "col6"));
        assertEquals(DEFAULT_WEIGHT, getFieldValue(prefix + "col2:weight"));

        startInstance(instanceName);
        assertEquals(STATUS_RUNNING, getText(prefix + "col6"));

        stopInstance(instanceName);
        assertEquals(STATUS_STOPPED, getText(prefix + "col6"));

        deleteStandAloneInstance(instanceName);
    }

    @Test
    public void testProperties() {
        String instanceName = INSTANCE_PREFIX + generateRandomString();
        createStandAloneInstance(instanceName);

        clickAndWait(getLinkIdByLinkText(ID_INSTANCE_TABLE, instanceName), TRIGGER_GENERAL_INFO_PAGE);
        clickAndWait(ID_INSTANCE_PROP_TAB, TRIGGER_SYS_PROPS);
        int sysPropCount = addTableRow("propertyForm:sysPropsTable", "propertyForm:sysPropsTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:overrideValCol:overrideVal", "foo=bar");
        // FIXME: The app needs to be fixed here. should show success message
        clickAndWait("propertyForm:clusterSysPropsPage:topButtons:topButtons:saveButton", TRIGGER_SYS_PROPS);
        sleep(1000); // grr! FIXME

        // Go to instance props page
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:instanceProps", "Additional Properties (0)"); // FIXME
//        waitForPageLoad(TRIGGER_SYS_PROPS, TIMEOUT, true);

        int instancePropCount = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "foo=bar");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        // Verify that properties were persisted
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:configProps", TRIGGER_SYS_PROPS);
        sleep(1000); // grr
        assertTableRowCount("propertyForm:sysPropsTable", sysPropCount);
        clickAndWait("propertyForm:standaloneInstanceTabs:standaloneProp:instanceProps", "Additional Properties (1)"); // FIXME
        assertTableRowCount("propertyForm:basicTable", instancePropCount);

        deleteStandAloneInstance(instanceName);
    }

    @Test
    public void testStandaloneInstanceResourcesPage() {
        final String jndiName = "jdbcResource"+generateRandomString();
        String target = INSTANCE_PREFIX + generateRandomString();
        final String description = "devtest test for standalone instance->resources page- " + jndiName;
        final String tableID = "propertyForm:resourcesTable";

        JdbcTest jdbcTest = new JdbcTest();
        jdbcTest.createJDBCResource(jndiName, description, target, MonitoringTest.TARGET_STANDALONE_TYPE);

        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", TRIGGER_INSTANCES_PAGE);
        clickAndWait(getLinkIdByLinkText(ID_INSTANCE_TABLE, target), TRIGGER_GENERAL_INFO_PAGE);
        clickAndWait("propertyForm:standaloneInstanceTabs:resources", EnterpriseServerTest.TRIGGER_RESOURCES);
        assertTrue(isTextPresent(jndiName));

        int jdbcCount = getTableRowCountByValue(tableID, "JDBC Resources", "col3:type");
        int customCount = getTableRowCountByValue(tableID, "Custom Resources", "col3:type");

        EnterpriseServerTest adminServerTest = new EnterpriseServerTest();
        selectDropdownOption("propertyForm:resourcesTable:topActionsGroup1:filter_list", "Custom Resources");
        adminServerTest.waitForTableRowCount(tableID, customCount);

        selectDropdownOption("propertyForm:resourcesTable:topActionsGroup1:filter_list", "JDBC Resources");
        adminServerTest.waitForTableRowCount(tableID, jdbcCount);

        selectTableRowByValue("propertyForm:resourcesTable", jndiName);
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:button1");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:button1");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:button1");

        /*selectDropdownOption("propertyForm:resourcesTable:topActionsGroup1:actions", "JDBC Resources");
        waitForPageLoad(JdbcTest.TRIGGER_NEW_JDBC_RESOURCE, true);
        clickAndWait("form:propertyContentPage:topButtons:cancelButton", JdbcTest.TRIGGER_JDBC_RESOURCES);*/

        jdbcTest.deleteJDBCResource(jndiName, target, MonitoringTest.TARGET_STANDALONE_TYPE);
    }

    public void createStandAloneInstance(String instanceName){
        gotoStandaloneInstancesPage();
        clickAndWait(ID_INSTANCE_TABLE_NEW_BUTTON, TRIGGER_NEW_PAGE );
        setFieldValue(ID_INSTANCE_NAME_TEXT, instanceName);
        selectDropdownOption(ID_INSTANCE_NODE_TEXT, NODE_NAME);
        selectDropdownOption(ID_INSTANCE_CONFIG_SELECT, "default-config");
        markCheckbox(ID_INSTANCE_CONFIG_OPTION);
        clickAndWait(ID_INSTANCE_NEW_PAGE_BUTTON, TRIGGER_INSTANCES_PAGE);
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertTrue(isTextPresent(instanceName));
    }

    public void deleteStandAloneInstance(String instanceName) {
        gotoStandaloneInstancesPage();
        rowActionWithConfirm(ID_INSTANCE_TABLE_STOP_BUTTON, ID_INSTANCE_TABLE, instanceName);
        deleteRow(ID_INSTANCE_TABLE_DELETE_BUTTON, ID_INSTANCE_TABLE, instanceName);
    }

    public void deleteAllStandaloneInstances() {
        gotoStandaloneInstancesPage();
        if (getTableRowCount(ID_INSTANCE_TABLE) == 0) {
            return;
        }

        this.selectAllTableRows(ID_INSTANCE_TABLE, 0);
        chooseOkOnNextConfirmation();
        pressButton("propertyForm:instancesTable:topActionsGroup1:button3");
        waitForButtonDisabled("propertyForm:instancesTable:topActionsGroup1:button3");
        getConfirmation();


        /*
        // Stop all instances
        if (selectTableRowsByValue(ID_INSTANCE_TABLE, STATUS_RUNNING, "col0", "col6") > 0) {
            waitForButtonEnabled(ID_INSTANCE_TABLE_STOP_BUTTON);
            chooseOkOnNextConfirmation();
            pressButton(ID_INSTANCE_TABLE_STOP_BUTTON);
            if (isConfirmationPresent()) {
                getConfirmation();
            }
            this.waitForButtonDisabled(ID_INSTANCE_TABLE_STOP_BUTTON);
        }
         */

        // Delete all instances
        deleteAllTableRows(ID_INSTANCE_TABLE, 0);  //"propertyForm:instancesTable");
    }

    public void startInstance(String instanceName) {
        rowActionWithConfirm(ID_INSTANCE_TABLE_START_BUTTON, ID_INSTANCE_TABLE, instanceName);
        waitForCondition("document.getElementById('" + ID_INSTANCE_TABLE_START_BUTTON + "').value != 'Processing...'", 300000);
        String prefix = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        assertEquals(STATUS_RUNNING, getText(prefix + "col6"));
    }

    public void stopInstance(String instanceName) {
        String rowId = getTableRowByValue(ID_INSTANCE_TABLE, instanceName, "col1");
        String status = getText(rowId+"col6");
        if (! STATUS_STOPPED.equals(status)) {
            rowActionWithConfirm(ID_INSTANCE_TABLE_STOP_BUTTON, ID_INSTANCE_TABLE, instanceName);
            waitForCondition("document.getElementById('" + ID_INSTANCE_TABLE_STOP_BUTTON + "').value != 'Processing...'", 300000);
        }
    }

    public  void gotoStandaloneInstancesPage() {
        reset();
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", TRIGGER_INSTANCES_PAGE);
    }

}
