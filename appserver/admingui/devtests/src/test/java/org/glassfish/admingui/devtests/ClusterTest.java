/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author anilam
 */
public class ClusterTest extends BaseSeleniumTestClass {

    public static final String ID_CLUSTERS_TABLE = "propertyForm:clustersTable";
    public static final String ID_CLUSTERS_TABLE_DELETE_BUTTON = "propertyForm:clustersTable:topActionsGroup1:button1";
    public static final String ID_CLUSTERS_TABLE_SELECT_ALL_BUTTON = "propertyForm:clustersTable:_tableActionsTop:_selectMultipleButton";
    public static final String ID_CLUSTERS_TABLE_STOP_BUTTON = "propertyForm:clustersTable:topActionsGroup1:button3";
    public static final String TRIGGER_CLUSTER_NO_RUNNING_INSTANCES = "i18ncs.cluster.migrateEjbTimersNoRunningInstance";
    public static final String TRIGGER_CONFIGURATION_TEXT = "i18n.configuration.pageTitleHelp";
    public static final String TRIGGER_MIGRATE_EJB_TIMERS = "i18ncs.cluster.migrateEjbTimersHelp";
    public static final String TRIGGER_CLUSTER_PAGE = "i18ncs.clusters.PageTitleHelp";
    public static final String TRIGGER_NEW_CLUSTER_PAGE = "i18ncs.clusterNew.PageTitleHelp";
    public static final String TRIGGER_CLUSTER_GENERAL_PAGE = "i18ncs.cluster.GeneralTitleHelp";
    public static final String TRIGGER_CLUSTER_INSTANCE_NEW_PAGE = "i18ncs.clusterInstanceNew.PageTitleHelp";
    public static final String TRIGGER_CLUSTER_INSTANCES_PAGE = "i18ncs.cluster.InstancesTitleHelp";
    public static final String TRIGGER_CLUSTER_RESOURCES_PAGE = "i18ncs.cluster.ResourcesTitleHelp";
    public static final String TRIGGER_CLUSTER_SYSTEM_PROPERTIES = "i18ncs.cluster.ClusterSystemProperties";

    @Test
    public void testStartAndStopClusterWithOneInstance() {
        String clusterName = "clusterName" + generateRandomString();
        String instanceName1 = "instanceName" + generateRandomString();
        createCluster(clusterName, instanceName1);

        // Verify cluster information in table
        String prefix = getTableRowByValue(ID_CLUSTERS_TABLE, clusterName, "col1");
        assertEquals(clusterName, getText(prefix + "col1:link"));
        assertEquals(clusterName + "-config", getText(prefix + "col2:configlink"));
        assertEquals(instanceName1, getText(prefix + "col3:iLink"));

        // Start the cluster and verify
        rowActionWithConfirm("propertyForm:clustersTable:topActionsGroup1:button2", ID_CLUSTERS_TABLE, clusterName);
        waitForButtonDisabled("propertyForm:clustersTable:topActionsGroup1:button2");
        prefix = getTableRowByValue(ID_CLUSTERS_TABLE, clusterName, "col1");
        assertTrue((getText(prefix + "col3").indexOf("Running") != -1));

        // Stop the cluster and verify
        rowActionWithConfirm("propertyForm:clustersTable:topActionsGroup1:button3", ID_CLUSTERS_TABLE, clusterName);
        waitForButtonDisabled("propertyForm:clustersTable:topActionsGroup1:button3");
        assertTrue((getText(prefix + "col3").indexOf("Stopped") != -1));

        deleteCluster(clusterName);
    }

    @Test
    public void testMigrateEjbTimers() {
        String clusterName = "clusterName" + generateRandomString();
        String instanceName1 = "instanceName" + generateRandomString();
        String instanceName2 = "instanceName" + generateRandomString();
        createCluster(clusterName, instanceName1, instanceName2);

        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, clusterName), TRIGGER_CLUSTER_GENERAL_PAGE);
        clickAndWait("propertyForm:clusterTabs:clusterInst", TRIGGER_CLUSTER_INSTANCES_PAGE);

        startClusterInstance(instanceName1);

        clickAndWait("propertyForm:clusterTabs:general", TRIGGER_CLUSTER_GENERAL_PAGE);
        clickAndWait("propertyForm:migrateTimesButton", TRIGGER_MIGRATE_EJB_TIMERS);
        Assert.assertFalse(isTextPresent(TRIGGER_CLUSTER_NO_RUNNING_INSTANCES));
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:clusterSourceProp:source", instanceName2);
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:clusterDestProp:dest", instanceName1);

        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", "Migrated 0 timers");
        clickAndWait("propertyForm:clusterTabs:clusterInst", TRIGGER_CLUSTER_INSTANCES_PAGE);

//        stopClusterInstance(instanceName1);
        deleteCluster(clusterName);
    }

    @Test
    public void verifyClusterGeneralInformationPage() {
        String clusterName = "clusterName" + generateRandomString();
        String instanceName1 = "instanceName" + generateRandomString();
        String instanceName2 = "instanceName" + generateRandomString();
        createCluster(clusterName, instanceName1, instanceName2);

        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, clusterName), TRIGGER_CLUSTER_GENERAL_PAGE);
        assertEquals(clusterName, getText("propertyForm:propertySheet:propertSectionTextField:clusterNameProp:clusterName"));

        //ensure config link is fine.
        //TODO:  how to ensure thats the correct configuration page ?
        assertEquals(clusterName + "-config", getText("propertyForm:propertySheet:propertSectionTextField:configNameProp:configlink"));
        clickAndWait("propertyForm:propertySheet:propertSectionTextField:configNameProp:configlink", TRIGGER_CONFIGURATION_TEXT);

        //Back to the Clusters page,  ensure default value is there.
        clickAndWait("treeForm:tree:clusterTreeNode:clusterTreeNode_link", TRIGGER_CLUSTER_PAGE);
        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, clusterName), TRIGGER_CLUSTER_GENERAL_PAGE);
        //TODO: should try to use the String key
        assertEquals("2 instances are stopped", getText("propertyForm:propertySheet:propertSectionTextField:instanceStatusProp:instanceStatusStopped"));

        //change value
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:gmsMulticastPort:gmsMulticastPort", "12345");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:gmsMulticastAddress:gmsMulticastAddress", "123.234.456.88");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:GmsBindInterfaceAddress:GmsBindInterfaceAddress", "${ABCDE}");
        clearCheckbox("propertyForm:propertySheet:propertSectionTextField:gmsEnabledProp:gmscb");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        //ensure value is saved correctly
        assertEquals("12345", getFieldValue("propertyForm:propertySheet:propertSectionTextField:gmsMulticastPort:gmsMulticastPort"));
        assertEquals("123.234.456.88", getFieldValue("propertyForm:propertySheet:propertSectionTextField:gmsMulticastAddress:gmsMulticastAddress"));
        assertEquals("${ABCDE}", getFieldValue("propertyForm:propertySheet:propertSectionTextField:GmsBindInterfaceAddress:GmsBindInterfaceAddress"));
        assertEquals(false, isChecked("propertyForm:propertySheet:propertSectionTextField:gmsEnabledProp:gmscb"));
        
        deleteCluster(clusterName);
    }

    @Test
    public void testClusterInstancesTab() {
        String clusterName = "clusterName" + generateRandomString();
        String instanceName1 = "instanceName" + generateRandomString();
        String instanceName2 = "instanceName" + generateRandomString();
        createCluster(clusterName, instanceName1);

        gotoClusterInstancesPage(clusterName);

        assertTrue(isTextPresent(instanceName1));

        clickAndWait("propertyForm:instancesTable:topActionsGroup1:newButton", TRIGGER_CLUSTER_INSTANCE_NEW_PAGE);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText", instanceName2);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_CLUSTER_INSTANCES_PAGE);
        assertTrue(isTextPresent(instanceName2));

        deleteCluster(clusterName);
    }

    @Test
    public void testProperties() {
        String clusterName = "clusterName" + generateRandomString();
        String instanceName1 = "instanceName" + generateRandomString();
        createCluster(clusterName, instanceName1);

        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, clusterName), TRIGGER_CLUSTER_GENERAL_PAGE);
        assertEquals(clusterName, getText("propertyForm:propertySheet:propertSectionTextField:clusterNameProp:clusterName"));

        // Go to properties tab
        clickAndWait("propertyForm:clusterTabs:clusterProps", TRIGGER_CLUSTER_SYSTEM_PROPERTIES);
        int sysPropCount = addTableRow("propertyForm:sysPropsTable", "propertyForm:sysPropsTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:overrideValCol:overrideVal", "value");
        clickAndWait("propertyForm:clusterSysPropsPage:topButtons:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        // Go to cluster props page
        pressButton("propertyForm:clusterTabs:clusterProps:clusterInstanceProps");
        waitForPageLoad(TRIGGER_CLUSTER_SYSTEM_PROPERTIES, TIMEOUT, true);

        int clusterPropCount = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        // Verify that properties were persisted
        clickAndWait("propertyForm:clusterTabs:clusterProps:clusterSystemProps", TRIGGER_CLUSTER_SYSTEM_PROPERTIES);
        assertTableRowCount("propertyForm:sysPropsTable", sysPropCount);
        pressButton("propertyForm:clusterTabs:clusterProps:clusterInstanceProps");
        waitForPageLoad(TRIGGER_CLUSTER_SYSTEM_PROPERTIES, TIMEOUT, true);
        assertTableRowCount("propertyForm:basicTable", clusterPropCount);

        deleteCluster(clusterName);
    }

    @Test
    public void testMultiDeleteClusters() {
        String clusterName1 = "cluster" + generateRandomString();
        String clusterName2 = "cluster" + generateRandomString();

        createCluster(clusterName1);
        createCluster(clusterName2);
        deleteAllClusters();

        assertEquals(0, getTableRowsByValue(ID_CLUSTERS_TABLE, clusterName1, "col1").size());
        assertEquals(0, getTableRowsByValue(ID_CLUSTERS_TABLE, clusterName2, "col1").size());
    }

    @Test
    public void testClusterWithJmsOptions() {
        String clusterName = "cluster" + generateRandomString();
        try {
            gotoClusterPage();
            clickAndWait("propertyForm:clustersTable:topActionsGroup1:newButton", TRIGGER_NEW_CLUSTER_PAGE);
            setFieldValue("propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText", clusterName);

            markCheckbox("propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optCustom:optCustom_label");

            waitForPageLoad("i18ncs.cluster.jms.ClusterTypeName", TIMEOUT);

            clickAndWait("propertyForm:jmsTypePropertySheet:jmsTypeSection:jmsTypeProp:optLocal", "i18ncs.cluster.jms.mqClusterTypeEnhanced");
            selenium.check("propertyForm:jmsPropertySheet:configureJmsClusterSection:ClusterTypeProp:optConventional");
    //        selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:ClusterTypeProp:clusterType", "Conventional");

            selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:ConfigStoreTypeProp:configStoreType", "Master Broker");
            selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:MessageStoreTypeProp:messageStoreType", "File");
            setFieldValue("propertyForm:jmsPropertySheet:configureJmsClusterSection:PropertiesProp:properties", "prop1=value1:prop2=value2\\:with\\:colons:prop3=value3");

        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                "Server Instances to Be Created");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in1");
        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                "Server Instances to Be Created");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in2");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_CLUSTER_PAGE);
            assertTrue(isTextPresent(clusterName));
        } finally {
            deleteCluster(clusterName);
        }
    }

    @Test
    public void testClusterWithEnhancedJmsOptions() {
        String clusterName = "cluster" + generateRandomString();
        try {
            gotoClusterPage();
            clickAndWait("propertyForm:clustersTable:topActionsGroup1:newButton", TRIGGER_NEW_CLUSTER_PAGE);
            setFieldValue("propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText", clusterName);
            markCheckbox("propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optCustom:optCustom_label");
            waitForPageLoad("i18ncs.cluster.jms.ClusterTypeName", TIMEOUT);

            clickAndWait("propertyForm:jmsTypePropertySheet:jmsTypeSection:jmsTypeProp:optLocal", "i18ncs.cluster.jms.mqClusterTypeEnhanced");
            selenium.check("propertyForm:jmsPropertySheet:configureJmsClusterSection:ClusterTypeProp:optEnhanced");

            setFieldValue("propertyForm:jmsPropertySheet:configureJmsClusterSection:DbVendorProp:dbVendor", "mysql");
            setFieldValue("propertyForm:jmsPropertySheet:configureJmsClusterSection:DbUserProp:dbUser", "root");
            setFieldValue("propertyForm:jmsPropertySheet:configureJmsClusterSection:DbUrlProp:dbUrl", "jdbc:mysql://hostname:portno/dbname?password=xxx");

            addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                    "Server Instances to Be Created");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in1");
            addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                    "Server Instances to Be Created");
            setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in2");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_CLUSTER_PAGE);
            assertTrue(isTextPresent(clusterName));
        } finally {
            deleteCluster(clusterName);
        }
    }

    @Test
    public void testClusterWithBadJmsOptions() {
        String clusterName = "cluster" + generateRandomString();
        gotoClusterPage();
        clickAndWait("propertyForm:clustersTable:topActionsGroup1:newButton", TRIGGER_NEW_CLUSTER_PAGE);
        setFieldValue("propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText", clusterName);
        markCheckbox("propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optCustom:optCustom_label");
        waitForPageLoad("i18ncs.cluster.jms.ClusterTypeName", TIMEOUT);

        clickAndWait("propertyForm:jmsTypePropertySheet:jmsTypeSection:jmsTypeProp:optLocal", "i18ncs.cluster.jms.mqClusterTypeEnhanced");
        selenium.check("propertyForm:jmsPropertySheet:configureJmsClusterSection:ClusterTypeProp:optConventional");
//        selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:ClusterTypeProp:clusterType", "Conventional");
        
        selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:ConfigStoreTypeProp:configStoreType", "Master Broker");
        selectDropdownOption("propertyForm:jmsPropertySheet:configureJmsClusterSection:MessageStoreTypeProp:messageStoreType", "JDBC");

        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                "Server Instances to Be Created");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in1");
        addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                "Server Instances to Be Created");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", clusterName + "in2");

        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", "An error occurred");
        gotoClusterPage();
        assertFalse(isTextPresent(clusterName));
    }

    public void gotoClusterPage() {
        reset();
        clickAndWait("treeForm:tree:clusterTreeNode:clusterTreeNode_link", TRIGGER_CLUSTER_PAGE);
    }

    public void gotoClusterInstancesPage(String clusterName) {
        gotoClusterPage();
        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, clusterName), TRIGGER_CLUSTER_GENERAL_PAGE);
        clickAndWait("propertyForm:clusterTabs:clusterInst", TRIGGER_CLUSTER_INSTANCES_PAGE);
    }

    @Test
    public void testClusterResourcesPage() {
        final String jndiName = "jdbcResource" + generateRandomString();
        String target = "cluster" + generateRandomString();
        final String description = "devtest test for cluster->resources page- " + jndiName;
        final String tableID = "propertyForm:resourcesTable";

        JdbcTest jdbcTest = new JdbcTest();
        jdbcTest.createJDBCResource(jndiName, description, target, MonitoringTest.TARGET_CLUSTER_TYPE);

        clickAndWait("treeForm:tree:clusterTreeNode:clusterTreeNode_link", TRIGGER_CLUSTER_PAGE);
        clickAndWait(getLinkIdByLinkText(ID_CLUSTERS_TABLE, target), TRIGGER_CLUSTER_GENERAL_PAGE);
        clickAndWait("propertyForm:clusterTabs:clusterResources", TRIGGER_CLUSTER_RESOURCES_PAGE);
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
        jdbcTest.deleteJDBCResource(jndiName, target, MonitoringTest.TARGET_CLUSTER_TYPE);
    }

    public void createCluster(String clusterName, String... instanceNames) {
        gotoClusterPage();
        clickAndWait("propertyForm:clustersTable:topActionsGroup1:newButton", TRIGGER_NEW_CLUSTER_PAGE);
        setFieldValue("propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText", clusterName);

        if (instanceNames != null) {
            for (String instanceName : instanceNames) {
                if (instanceName != null && !instanceName.equals("")) {
                    addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton",
                            "Server Instances to Be Created");
                    setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:name", instanceName);
//                    createClusterInstance(clusterName, instanceName);
                }
            }
        }
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_CLUSTER_PAGE);
    }

    public void createClusterInstance(String clusterName, String instanceName) {
        gotoClusterInstancesPage(clusterName);
        clickAndWait("propertyForm:instancesTable:topActionsGroup1:newButton", "Name of the node machine on which the server instance will reside");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText", instanceName);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_CLUSTER_INSTANCES_PAGE);
        Assert.assertNotNull(this.getTableRowByValue("propertyForm:instancesTable", instanceName, "col1"));
    }

    public void deleteCluster(String clusterName) {
        gotoClusterPage();
        rowActionWithConfirm(ID_CLUSTERS_TABLE_STOP_BUTTON, ID_CLUSTERS_TABLE, clusterName);
        deleteRow(ID_CLUSTERS_TABLE_DELETE_BUTTON, ID_CLUSTERS_TABLE, clusterName);
    }

    public void deleteAllClusters() {
        gotoClusterPage();

        if (getTableRowCount(ID_CLUSTERS_TABLE) == 0) {
            return;
        }


        List<String> rows = getTableRowsByValue(ID_CLUSTERS_TABLE, "Running", "col3");
        if (!rows.isEmpty()) {
            // Stop all instances
            for (String row : rows) {
                String clusterName = getText(row + ":col1");
                this.selectTableRowByValue(ID_CLUSTERS_TABLE, clusterName);
                //rowActionWithConfirm(ID_CLUSTERS_TABLE_STOP_BUTTON, ID_CLUSTERS_TABLE, clusterName);
//                selenium.click(ID_CLUSTERS_TABLE_SELECT_ALL_BUTTON);
//                waitForButtonEnabled(ID_CLUSTERS_TABLE_STOP_BUTTON);
//                selenium.chooseOkOnNextConfirmation();
//                selenium.click(ID_CLUSTERS_TABLE_STOP_BUTTON);
//                if (selenium.isConfirmationPresent()) {
//                    selenium.getConfirmation();
//                }
//                waitForButtonDisabled(ID_CLUSTERS_TABLE_STOP_BUTTON);
            }
            waitForButtonEnabled(ID_CLUSTERS_TABLE_STOP_BUTTON);
            chooseOkOnNextConfirmation();
            pressButton(ID_CLUSTERS_TABLE_STOP_BUTTON);
            if (isConfirmationPresent()) {
                getConfirmation();
            }
            waitForButtonDisabled(ID_CLUSTERS_TABLE_STOP_BUTTON);
        }

        deleteAllTableRows(ID_CLUSTERS_TABLE);
        // Delete all clusters
        // FIXME: We're iterating through these one at a time, because we are occasionally getting a
        // "false" failure, where a cluster was deleted, but the server was unable to delete
        // some files, so it returns an error, which causes the loop in deleteAllTableRows()
        // to exit early
//        List<String> clusters = getTableColumnValues(ID_CLUSTERS_TABLE, "col1");
//        for (String clusterName : clusters) {
////            gotoClusterPage();
//            rowActionWithConfirm(ID_CLUSTERS_TABLE_DELETE_BUTTON, ID_CLUSTERS_TABLE, clusterName);
//        }
        assertTableRowCount(ID_CLUSTERS_TABLE, 0);
    }

    public void startClusterInstance(String instanceName) {
        rowActionWithConfirm("propertyForm:instancesTable:topActionsGroup1:button2", "propertyForm:instancesTable", instanceName);
        waitForCondition("document.getElementById('propertyForm:instancesTable:topActionsGroup1:button2').value != 'Processing...'", 300000);
    }

    public void stopClusterInstance(String instanceName) {
        rowActionWithConfirm("propertyForm:instancesTable:topActionsGroup1:button3", "propertyForm:instancesTable", instanceName);
        waitForCondition("document.getElementById('propertyForm:instancesTable:topActionsGroup1:button3').value != 'Processing...'", 300000);
    }

    public void stopAllClusterInstances(String clusterName) {
        gotoClusterInstancesPage(clusterName);

        if (isTextPresent("Server Instances (0)")) {
            return;
        }

        if (selectTableRowsByValue("propertyForm:instancesTable", "Running", "col0", "col6") > 0) {
            waitForButtonEnabled("propertyForm:instancesTable:topActionsGroup1:button3");
            chooseOkOnNextConfirmation();
            pressButton("propertyForm:instancesTable:topActionsGroup1:button3");
            if (isConfirmationPresent()) {
                getConfirmation();
            }
            this.waitForButtonDisabled("propertyForm:instancesTable:topActionsGroup1:button3");
        }
    }
}
