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

public class AvailabilityServiceTest extends BaseSeleniumTestClass {
    public static final String ID_AVAILABILITY_SERVICE_TREE_NODE = "treeForm:tree:configurations:default-config:availabilityService:availabilityService_link";
    private static final String ID_DEFAULT_CONFIG_TURNER = "treeForm:tree:configurations:default-config:default-config_turner:default-config_turner_image";
    private static final String TRIGGER_AVAILABILTY_SERVICE_NODE = "i18ncs.tree.availsvc";
    private static final String TRIGGER_AVAILABILTY_SERVICE_PAGE = "i18ncs.availabilty.TitlePageHelp";
    private static final String TRIGGER_WEB_AVAILABILTY = "i18n_web.availability.webContainerAvailabilityInfo";
    private static final String TRIGGER_EJB_AVAILABILTY = "i18n_ejb.availability.ejbContainerAvailabilityInfo";
    private static final String TRIGGER_JMS_AVAILABILTY = "i18njms.availability.jmsAvailabilityInfo";
//    private static final String TRIGGER_SUCCESS_MSG = "New values successfully saved";

    @Test
    public void testAvailabilityService() {
        // Expand node
        if (!isTextPresent(TRIGGER_AVAILABILTY_SERVICE_NODE)) {
            clickAndWait(ID_DEFAULT_CONFIG_TURNER, TRIGGER_AVAILABILTY_SERVICE_NODE);
        }
        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE, TRIGGER_AVAILABILTY_SERVICE_PAGE);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
    }

    @Test
    public void testWebContainerAvailability() {
        if (!isTextPresent(TRIGGER_AVAILABILTY_SERVICE_NODE)) {
            clickAndWait(ID_DEFAULT_CONFIG_TURNER, TRIGGER_AVAILABILTY_SERVICE_NODE);
        }
        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE, TRIGGER_AVAILABILTY_SERVICE_PAGE);
        clickAndWait("propertyForm:availabilityTabs:webAvailabilityTab", TRIGGER_WEB_AVAILABILTY);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
    }

    @Test
    public void testEjbContainerAvailability() {
        if (!isTextPresent(TRIGGER_AVAILABILTY_SERVICE_NODE)) {
            clickAndWait(ID_DEFAULT_CONFIG_TURNER, TRIGGER_AVAILABILTY_SERVICE_NODE);
        }
        clickAndWait(ID_AVAILABILITY_SERVICE_TREE_NODE, TRIGGER_AVAILABILTY_SERVICE_PAGE);
        clickAndWait("propertyForm:availabilityTabs:ejbAvailabilityTab", TRIGGER_EJB_AVAILABILTY);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
    }

    @Test
    public void testJMSAvailability() {
        final String configName = "Config-" + generateRandomString();
        MsgSecurityTest msgS = new MsgSecurityTest();
        msgS.copyConfig("default-config", configName);

//        if (!isTextPresent(TRIGGER_AVAILABILTY_SERVICE_NODE)) {
//            clickAndWait(ID_DEFAULT_CONFIG_TURNER, TRIGGER_AVAILABILTY_SERVICE_NODE);
//        }

        clickAndWait(getAvailabilityLink(configName), TRIGGER_AVAILABILTY_SERVICE_PAGE);
        clickAndWait("propertyForm:availabilityTabs:jmsAvailabilityTab", TRIGGER_JMS_AVAILABILTY);
        markCheckbox("propertyForm:propertySheet:propertSectionTextField:AvailabilityEnabledProp:avail");
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:ConfigStoreTypeProp:ConfigStoreType", "shareddb");
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:MessageStoreTypeProp:MessageStoreType", "file");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbVendorProp:DbVendor", "Vendor");
	setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUserNameProp:DbUserName", "USERNAME");
	setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbPasswordProp:DbPassword", "PSWD");
	setFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUrlProp:DbUrl", "URL");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("propertyForm:availabilityTabs:availabilityTab", TRIGGER_AVAILABILTY_SERVICE_PAGE);
        clickAndWait("propertyForm:availabilityTabs:jmsAvailabilityTab", TRIGGER_JMS_AVAILABILTY);

        assertEquals(getSelectedValue("propertyForm:propertySheet:propertSectionTextField:ConfigStoreTypeProp:ConfigStoreType"), "shareddb");
        assertEquals(getSelectedValue("propertyForm:propertySheet:propertSectionTextField:MessageStoreTypeProp:MessageStoreType"), "file");
        assertEquals("Vendor", getFieldValue("propertyForm:propertySheet:propertSectionTextField:DbVendorProp:DbVendor"));
	assertEquals("USERNAME", getFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUserNameProp:DbUserName"));
	assertEquals( "PSWD", getFieldValue("propertyForm:propertySheet:propertSectionTextField:DbPasswordProp:DbPassword"));
	assertEquals( "URL", getFieldValue("propertyForm:propertySheet:propertSectionTextField:DbUrlProp:DbUrl"));

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertTableRowCount("propertyForm:basicTable", count);
    }

    private static String getAvailabilityLink(String configName){
       return  "treeForm:tree:configurations:" + configName + ":availabilityService:availabilityService_link";
    }
}
