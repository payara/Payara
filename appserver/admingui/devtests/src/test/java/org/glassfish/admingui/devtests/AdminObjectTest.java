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

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AdminObjectTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_ADMIN_OBJECT_RESOURCES = "i18njca.adminObjectResources.pageTitleHelp";

    private static final String TRIGGER_NEW_ADMIN_OBJECT_RESOURCE = "i18njca.adminObject.NewPageTitleHelp";

    public static final String TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE = "i18njca.adminObjectResource.editPageHelp";

    @Test
    public void testAdminObjectResources() throws Exception {
        final String resName = "adminObject" + generateRandomString();
        final String description = "Admin Object Resource - " + resName;

        StandaloneTest standaloneTest = new StandaloneTest();
        ClusterTest clusterTest = new ClusterTest();
        standaloneTest.deleteAllStandaloneInstances();
        clusterTest.deleteAllClusters();

        //Go to Admin Object Resources Page.
        clickAndWait("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link", TRIGGER_ADMIN_OBJECT_RESOURCES);

        //New Admin Object Resources
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_ADMIN_OBJECT_RESOURCE);

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resName);
        setFieldValue("form:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        //setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");
        int emptyCount = getTableRowCountByValue("form:basicTable", "", "col3:col1St", false);
        count = count - emptyCount;
        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_ADMIN_OBJECT_RESOURCES);

        assertTrue(isTextPresent(resName));
        assertTrue(isTextPresent(description));
        clickAndWait(getLinkIdByLinkText("propertyForm:resourcesTable", resName), TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_ADMIN_OBJECT_RESOURCES);

        testDisableButton(resName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button3",
                "propertyForm:propertySheet:propertSectionTextField:statusProp:enabled",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_ADMIN_OBJECT_RESOURCES,
                TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE,
                "off");
        testEnableButton(resName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button2",
                "propertyForm:propertySheet:propertSectionTextField:statusProp:enabled",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_ADMIN_OBJECT_RESOURCES,
                TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE,
                "on");

        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resName);
    }

    @Test
    public void testAdminObjectResourcesWithTargets() {
        final String resName = "adminObject" + generateRandomString();
        final String description = "Admin Object Resource - " + resName;
        final String instanceName = "standalone" + generateRandomString();
        final String enableStatus = "Enabled on 2 of 2 Target(s)";
        final String disableStatus = "Enabled on 0 of 2 Target(s)";

        StandaloneTest instanceTest = new StandaloneTest();
        instanceTest.createStandAloneInstance(instanceName);

        //Go to Admin Object Resources Page.
        clickAndWait("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link", TRIGGER_ADMIN_OBJECT_RESOURCES);

        //New Admin Object Resources
        clickAndWait("propertyForm:resourcesTable:topActionsGroup1:newButton", TRIGGER_NEW_ADMIN_OBJECT_RESOURCE);

        setFieldValue("form:propertySheet:propertSectionTextField:nameNew:name", resName);
        setFieldValue("form:propertySheet:propertSectionTextField:descriptionProp:descAdaptor", description);
        int count = addTableRow("form:basicTable", "form:basicTable:topActionsGroup1:addButton");

        setFieldValue("form:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("form:basicTable:rowGroup1:0:col3:col1St", "value");
        //setFieldValue("form:basicTable:rowGroup1:0:col4:col1St", "description");

        int emptyCount = getTableRowCountByValue("form:basicTable", "", "col3:col1St", false);
        count = count - emptyCount;

        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", instanceName);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", "server");
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");

        clickAndWait("form:propertyContentPage:topButtons:newButton", TRIGGER_ADMIN_OBJECT_RESOURCES);

        assertTrue(isTextPresent(resName));
        assertTrue(isTextPresent(description));
        clickAndWait(getLinkIdByLinkText("propertyForm:resourcesTable", resName), TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE);
        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_ADMIN_OBJECT_RESOURCES);

        testDisableButton(resName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button3",
                "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_ADMIN_OBJECT_RESOURCES,
                TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE,
                disableStatus);
        testEnableButton(resName,
                "propertyForm:resourcesTable",
                "propertyForm:resourcesTable:topActionsGroup1:button2",
                "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                "propertyForm:propertyContentPage:topButtons:cancelButton",
                TRIGGER_ADMIN_OBJECT_RESOURCES,
                TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE,
                enableStatus);

        testManageTargets("treeForm:tree:resources:Connectors:adminObjectResources:adminObjectResources_link",
                "propertyForm:resourcesTable",
                "propertyForm:targetTable:topActionsGroup1:button2",
                "propertyForm:targetTable:topActionsGroup1:button3",
                "propertyForm:propertySheet:propertSectionTextField:statusProp2:enabledStr",
                "propertyForm:resEditTabs:general",
                "propertyForm:resEditTabs:targetTab",
                TRIGGER_ADMIN_OBJECT_RESOURCES,
                TRIGGER_EDIT_ADMIN_OBJECT_RESOURCE,
                resName,
                instanceName);

        // Delete admin object resource
        deleteRow("propertyForm:resourcesTable:topActionsGroup1:button1", "propertyForm:resourcesTable", resName);

        //Delete the instance
        clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", instanceTest.TRIGGER_INSTANCES_PAGE);
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);
    }
}
