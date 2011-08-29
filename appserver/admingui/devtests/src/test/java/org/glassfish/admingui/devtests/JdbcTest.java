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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JdbcTest extends BaseSeleniumTestClass {
    public static final String TRIGGER_JDBC_CONNECTION_POOLS = "i18njdbc.jdbcConnectionPools.pageTitleHelp";
    public static final String TRIGGER_EDIT_JDBC_CONNECTION_POOL = "i18njdbc.jdbcConnection.editPageHelp";
    public static final String TRIGGER_ADVANCE_JDBC_CONNECTION_POOL = "i18njdbc.jdbcConnectionPool.advancePageTitleHelp";
    public static final String TRIGGER_PROPS_JDBC_CONNECTION_POOL = "i18njdbc.jdbcConnectionPool.propertyPageTitleHelp";
    public static final String TRIGGER_JDBC_RESOURCES = "i18njdbc.jdbcResources.pageTitleHelp";
    public static final String TRIGGER_EDIT_JDBC_RESOURCE = "i18njdbc.jdbcResource.editPageTitleHelp";
    public static final String TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_1 = "i18njdbc.jdbcConnection.step1PageHelp";
    public static final String TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_2 = "i18njdbc.jdbcConnection.step2PageHelp";
    public static final String TRIGGER_NEW_JDBC_RESOURCE = "i18njdbc.jdbcResource.newPageTitleHelp";

    @Test
    public void testPoolPing() {
        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:__TimerPool:link", "Edit JDBC Connection Pool");
        clickAndWait("propertyForm:propertyContentPage:ping", "Ping Succeeded");
    }

    @Test
    public void testCreatingConnectionPool() {
        final String poolName = "jdbcPool" + generateRandomString();
        final String description = "devtest test connection pool - " + poolName;

        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:connectionPoolResources_link", TRIGGER_JDBC_CONNECTION_POOLS);
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton", TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_1);

        setFieldValue("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name", poolName);
        selectDropdownOption("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:resTypeProp:resType", "javax.sql.DataSource");
        selectDropdownOption("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:dbProp:db", "Derby");
        clickAndWait("propertyForm:propertyContentPage:topButtons:nextButton", TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_2);

        setFieldValue("form2:sheet:generalSheet:descProp:desc", description);
        clickAndWait("form2:propertyContentPage:topButtons:finishButton", TRIGGER_JDBC_CONNECTION_POOLS);
        assertTrue(isTextPresent(poolName) && isTextPresent(description));

        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", poolName);
    }

    @Test
    public void testCreatingJdbcPoolWithoutDatabaseVendor() {
        final String poolName = "jdbcPool" + generateRandomString();
        final String description = "devtest test connection pool - " + poolName;

        clickAndWait("treeForm:tree:resources:JDBC:connectionPoolResources:connectionPoolResources_link", TRIGGER_JDBC_CONNECTION_POOLS);
        clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton", TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_1);

        setFieldValue("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name", poolName);
        selectDropdownOption("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:resTypeProp:resType", "javax.sql.ConnectionPoolDataSource");
        clickAndWait("propertyForm:propertyContentPage:topButtons:nextButton", TRIGGER_NEW_JDBC_CONNECTION_POOL_STEP_2);

        setFieldValue("form2:sheet:generalSheet:descProp:desc", description);
        setFieldValue("form2:sheet:generalSheet:dsProp:datasourceField", poolName + "DataSource");
        clickAndWait("form2:propertyContentPage:topButtons:finishButton", TRIGGER_JDBC_CONNECTION_POOLS);
        assertTrue(isTextPresent(poolName) && isTextPresent(description));

        deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", poolName);
    }

    @Test
    public void testJdbcResources() {
        final String jndiName = "jdbcResource" + generateRandomString();
        final String description = "devtest test jdbc resource - " + jndiName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllClusters();
       
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link", TRIGGER_JDBC_RESOURCES);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JDBC_RESOURCE);

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JDBC_RESOURCES);

        assertTrue(isTextPresent(jndiName));
        assertTrue(isTextPresent(description));

        clickAndWait(getLinkIdByLinkText("propertyForm:resourcesTable", jndiName), TRIGGER_EDIT_JDBC_RESOURCE);

        assertTableRowCount("propertyForm:basicTable", count);
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JDBC_RESOURCES);

        testDisableButton(jndiName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button3",
                "propertyForm:propertySheet:propertSectionTextField:statusProp:enabled",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_JDBC_RESOURCES,
                TRIGGER_EDIT_JDBC_RESOURCE,
                "off");
        testEnableButton(jndiName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button2",
                "propertyForm:propertySheet:propertSectionTextField:statusProp:enabled",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_JDBC_RESOURCES,
                TRIGGER_EDIT_JDBC_RESOURCE,
                "on");

        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", jndiName);
    }

    @Test
    public void testJdbcResourcesWithTargets() {
        final String jndiName = "jdbcResource" + generateRandomString();
        final String instanceName = "standalone" + generateRandomString();
        final String description = "devtest test jdbc resource with targets- " + jndiName;
        final String enableStatus = "Enabled on 2 of 2 Target(s)";
        final String disableStatus = "Enabled on 0 of 2 Target(s)";

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link", TRIGGER_JDBC_RESOURCES);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JDBC_RESOURCE);

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");

        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", instanceName);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", "server");
	pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JDBC_RESOURCES);

        assertTrue(isTextPresent(jndiName));
        assertTrue(isTextPresent(description));

        clickAndWait(getLinkIdByLinkText("propertyForm:resourcesTable", jndiName), TRIGGER_EDIT_JDBC_RESOURCE);

        assertTableRowCount("propertyForm:basicTable", count);
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JDBC_RESOURCES);

        testDisableButton(jndiName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button3",
                "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_JDBC_RESOURCES,
                TRIGGER_EDIT_JDBC_RESOURCE,
                disableStatus);
        testEnableButton(jndiName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button2",
                "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_JDBC_RESOURCES,
                TRIGGER_EDIT_JDBC_RESOURCE,
                enableStatus);
        testManageTargets("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link",
                          "propertyForm:resourcesTable",
                          "propertyForm:targetTable:topActionsGroup1:button2",
                          "propertyForm:targetTable:topActionsGroup1:button3",
                          "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                          "propertyForm:resEditTabs:general",
                          "propertyForm:resEditTabs:targetTab",
                          TRIGGER_JDBC_RESOURCES,
                          TRIGGER_EDIT_JDBC_RESOURCE,
                          jndiName,
                          instanceName);
                          
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", jndiName);
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", instanceTest.TRIGGER_INSTANCES_PAGE);
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
        assertFalse(tableContainsRow("propertyForm:instancesTable", "col0", instanceName));
    }

    public void createJDBCResource(String jndiName, String description, String target, String targetType) {
        if (targetType.equals(MonitoringTest.TARGET_STANDALONE_TYPE)) {
            StandaloneTest instanceTest = new StandaloneTest();
            instanceTest.createStandAloneInstance(target);
        } else if (targetType.equals(MonitoringTest.TARGET_CLUSTER_TYPE)) {
            ClusterTest clusterTest = new ClusterTest();
            clusterTest.createCluster(target);
        }
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link", TRIGGER_JDBC_RESOURCES);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JDBC_RESOURCE);

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", jndiName);
        setFieldValue("form:propertySheet:propertSectionTextField:descProp:desc", description);

        if (targetType.equals(MonitoringTest.TARGET_STANDALONE_TYPE)) {
            addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", target);
            pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        } else if (targetType.equals(MonitoringTest.TARGET_CLUSTER_TYPE)) {
            addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", target);
            pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        }
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JDBC_RESOURCES);

        assertTrue(isTextPresent(jndiName));
        assertTrue(isTextPresent(description));
    }

    public void deleteJDBCResource(String jndiName, String target, String targetType) {
        clickAndWait("treeForm:tree:resources:JDBC:jdbcResources:jdbcResources_link", TRIGGER_JDBC_RESOURCES);
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", jndiName);
        assertFalse(isTextPresent(jndiName));
        if (targetType.equals(MonitoringTest.TARGET_STANDALONE_TYPE)) {
            //Delete the instance
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", StandaloneTest.TRIGGER_INSTANCES_PAGE);
            deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", target);
            assertFalse(tableContainsRow("propertyForm:instancesTable", "col1", target));
        } else if (targetType.equals(MonitoringTest.TARGET_CLUSTER_TYPE)) {
            clickAndWait("treeForm:tree:clusterTreeNode:clusterTreeNode_link", ClusterTest.TRIGGER_CLUSTER_PAGE);
            deleteRow("propertyForm:clustersTable:topActionsGroup1:button1", "propertyForm:clustersTable", target);
            assertFalse(tableContainsRow("propertyForm:clustersTable", "col1", target));
        }
    }
}
