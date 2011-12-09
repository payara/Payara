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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


public class JmsResourcesTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_JMS_CONNECTION_FACTORIES = "i18njms.connectionFactories.pageTitleHelp";
    private static final String TRIGGER_NEW_JMS_CONN_FACT = "i18njms.connectionFactory.newPageTitleHelp";
    private static final String TRIGGER_EDIT_JMS_CONN_FACT = "i18njms.connectionFactory.editPageTitleHelp";
    private static final String TRIGGER_JMS_DESTINATION_RESOURCES = "i18njms.destinationResources.pageTitleHelp";
    private static final String TRIGGER_NEW_JMS_DEST_RES = "i18njms.jmsDestination.newPageTitleHelp";
    private static final String TRIGGER_EDIT_JMS_DEST_RES = "i18njms.jmsDestination.editPageTitleHelp";

    @Test
    public void testAddingConnectionFactories() throws Exception {
        final String poolName = "JMSConnFactory" + generateRandomString();
        final String description = "Test Pool - " + poolName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllClusters();

        clickAndWait("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link", TRIGGER_JMS_CONNECTION_FACTORIES);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JMS_CONN_FACT);

        setFieldValue("form:propertySheet:generalPropertySheet:jndiProp:jndiProp", poolName);
        selectDropdownOption("form:propertySheet:generalPropertySheet:resType:resType", "javax.jms.TopicConnectionFactory");
        setFieldValue("form:propertySheet:generalPropertySheet:descProp:descProp", description);
        selectDropdownOption("form:propertySheet:poolPropertySheet:transprop:trans", "LocalTransaction");
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JMS_CONNECTION_FACTORIES);
        assertTrue(isTextPresent(poolName));

        // This can't currently use testDisableButton/testEnableButton because the table is different from the others
        // The table should be fixed to be like the others (in terms of IDs) so the standard test API can be used here.
        selectTableRowByValue("propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");

        selectTableRowByValue("propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        deleteRow("propertyForm:resourcesTable:topActionsGroup1:deleteConnButton", "propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
    }

    @Test
    public void testAddingConnectionFactoriesWithTargets() throws Exception {
        final String poolName = "JMSConnFactory" + generateRandomString();
        final String description = "Test Pool - " + poolName;
        final String instanceName = "standalone" + generateRandomString();
        
        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link", TRIGGER_JMS_CONNECTION_FACTORIES);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JMS_CONN_FACT);

        setFieldValue("form:propertySheet:generalPropertySheet:jndiProp:jndiProp", poolName);
        selectDropdownOption("form:propertySheet:generalPropertySheet:resType:resType", "javax.jms.TopicConnectionFactory"); // i18n?
        setFieldValue("form:propertySheet:generalPropertySheet:descProp:descProp", description);
        selectDropdownOption("form:propertySheet:poolPropertySheet:transprop:trans", "LocalTransaction"); //i18n
        
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", instanceName);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", "server");
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JMS_CONNECTION_FACTORIES);
        assertTrue(isTextPresent(poolName));

        // This can't currently use testDisableButton/testEnableButton because the table is different from the others
        // The table should be fixed to be like the others (in terms of IDs) so the standard test API can be used here.
        selectTableRowByValue("propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");
     
        selectTableRowByValue("propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        testManageTargets("treeForm:tree:resources:jmsResources:jmsConnectionFactories:jmsConnectionFactories_link",
                          "propertyForm:resourcesTable",
                          "propertyForm:targetTable:topActionsGroup1:button2",
                          "propertyForm:targetTable:topActionsGroup1:button3",
                          "propertyForm:propertyContentPage:propertySheet:generalPropertySheet:statusProp2:enabledStr",
                          "propertyForm:resEditTabs:general",
                          "propertyForm:resEditTabs:targetTab",
                          TRIGGER_JMS_CONNECTION_FACTORIES,
                          TRIGGER_EDIT_JMS_CONN_FACT,
                          poolName,
                          instanceName);
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:deleteConnButton", "propertyForm:resourcesTable", poolName, "colSelect", "colPoolName");
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", instanceTest.TRIGGER_INSTANCES_PAGE);
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
        assertFalse(tableContainsRow("propertyForm:instancesTable", "col0", instanceName));
    }

    @Test
    public void testAddingDestinationResources() throws Exception {
        final String resourceName = "JMSDestination" + generateRandomString();
        final String description = "Test Destination - " + resourceName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllClusters();

        clickAndWait("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link", TRIGGER_JMS_DESTINATION_RESOURCES);
        sleep(1000);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JMS_DEST_RES);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:jndiProp:jndi", resourceName);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:nameProp:name", "somePhysicalDestination");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:descProp:desc", description);
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JMS_DESTINATION_RESOURCES);
        assertTrue(isTextPresent(resourceName) && isTextPresent(description));

        // This can't currently use testDisableButton/testEnableButton because the table is different from the others
        // The table should be fixed to be like the others (in terms of IDs) so the standard test API can be used here.
        selectTableRowByValue("propertyForm:resourcesTable", resourceName, "colSelect", "colName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");

        selectTableRowByValue("propertyForm:resourcesTable", resourceName, "colSelect", "colName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        deleteRow("propertyForm:resourcesTable:topActionsGroup1:deleteDestButton", "propertyForm:resourcesTable", resourceName, "colSelect", "colName");
    }

    @Test
    public void testAddingDestinationResourcesWithTargets() throws Exception {
        final String resourceName = "JMSDestination" + generateRandomString();
        final String instanceName = "standalone" + generateRandomString();
        final String description = "Test Destination - " + resourceName;

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        clickAndWait("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link", TRIGGER_JMS_DESTINATION_RESOURCES);
        sleep(1000);
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_JMS_DEST_RES);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:jndiProp:jndi", resourceName);
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:nameProp:name", "somePhysicalDestination");
        setFieldValue("form:propertyContentPage:propertySheet:propertSectionTextField:descProp:desc", description);

        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", instanceName);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", "server"); 
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_JMS_DESTINATION_RESOURCES);
        assertTrue(isTextPresent(resourceName) && isTextPresent(description));

        // This can't currently use testDisableButton/testEnableButton because the table is different from the others
        // The table should be fixed to be like the others (in terms of IDs) so the standard test API can be used here.
        selectTableRowByValue("propertyForm:resourcesTable", resourceName, "colSelect", "colName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:disableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:disableButton");

        selectTableRowByValue("propertyForm:resourcesTable", resourceName, "colSelect", "colName");
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        pressButton("propertyForm:resourcesTable:topActionsGroup1:enableButton");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:enableButton");

        testManageTargets("treeForm:tree:resources:jmsResources:jmsDestinationResources:jmsDestinationResources_link",
                          "propertyForm:resourcesTable",
                          "propertyForm:targetTable:topActionsGroup1:button2",
                          "propertyForm:targetTable:topActionsGroup1:button3",
                          "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                          "propertyForm:resEditTabs:general",
                          "propertyForm:resEditTabs:targetTab",
                          TRIGGER_JMS_DESTINATION_RESOURCES,
                          TRIGGER_EDIT_JMS_DEST_RES,
                          resourceName,
                          instanceName);
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:deleteDestButton", "propertyForm:resourcesTable", resourceName, "colSelect", "colName");
        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", instanceTest.TRIGGER_INSTANCES_PAGE);
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
        assertFalse(tableContainsRow("propertyForm:instancesTable", "col0", instanceName));
    }

/*
    @Test
    public void testAddingTransport() {
        selenium.click("treeForm:tree:configurations:server-config:networkConfig:transports:transports_link");
        verifyTrue(isTextPresent("Click New to define a new transport. Click the name of an existing transport to modify its settings."));
        selenium.click("propertyForm:configs:topActionsGroup1:newButton");
        verifyTrue(isTextPresent("New Transport"));
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", "transport");
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:ByteBufferType:ByteBufferType", "DIRECT");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes", "16384");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:AcceptorThreads:AcceptorThreads", "2");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:MaxConnectionsCount:MaxConnectionsCount", "8192");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdleKeyTimeoutSeconds:IdleKeyTimeoutSeconds", "60");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:ReadTimeoutMillis:ReadTimeoutMillis", "60000");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:SelectorPollTimeoutMillis:SelectorPollTimeoutMillis", "2000");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:WriteTimeoutMillis:WriteTimeoutMillis", "60000");
        selenium.click("propertyForm:propertyContentPage:topButtons:newButton");
        verifyTrue(isTextPresent("Click New to define a new transport. Click the name of an existing transport to modify its settings."));
        assertTrue(isTextPresent("transport"));
        selenium.click("propertyForm:configs:rowGroup1:0:col1:link");
        verifyTrue(isTextPresent("Edit Transport"));
        verifyTrue(selenium.isElementPresent("propertyForm:propertySheet:propertSectionTextField:ByteBufferType:ByteBufferType"));
        verifyEquals("16384", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes"));
        verifyEquals("2", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:AcceptorThreads:AcceptorThreads"));
        verifyEquals("8192", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:MaxConnectionsCount:MaxConnectionsCount"));
        verifyEquals("60", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:IdleKeyTimeoutSeconds:IdleKeyTimeoutSeconds"));
        verifyEquals("60000", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:ReadTimeoutMillis:ReadTimeoutMillis"));
        verifyEquals("2000", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:SelectorPollTimeoutMillis:SelectorPollTimeoutMillis"));
        verifyEquals("60000", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:WriteTimeoutMillis:WriteTimeoutMillis"));
        selenium.click("propertyForm:propertyContentPage:topButtons:cancelButton");
        verifyTrue(isTextPresent("Click New to define a new transport. Click the name of an existing transport to modify its settings."));
        selenium.click("propertyForm:configs:rowGroup1:0:col0:select");
        selenium.click("propertyForm:configs:topActionsGroup1:button1");
        assertTrue(selenium.getConfirmation().matches("^Selected Transport\\(s\\) will be deleted\\.  Continue[\\s\\S]$"));

    }
*/
}
