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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IiopListenerTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_IIOP_LISTENERS = "i18n_corba.iiopListeners.ListPageHelp";
    private static final String TRIGGER_NEW_IIOP_LISTENER = "i18n_corba.iiopListener.newPageTitle";
    private static final String TRIGGER_EDIT_IIOP_LISTENER = "i18n_corba.iiopListener.editPageTitle";
    private static final String TRIGGER_ORB = "i18n_corba.orb.OrbInfo";
    private static final String TRIGGER_EDIT_IIOP_SSL = "i18n_corba.sslPageTitleHelp";

    @Test
    public void testAddIiopListener() {
        final String iiopName = "testIiopListener" + generateRandomString();
        final String networkAddress = "0.0.0.0";
        final String listenerPort = Integer.toString(generateRandomNumber(32768));;
        final String certName = "s1as";

        clickAndWait("treeForm:tree:configurations:server-config:orb:iiopListeners:iiopListeners_link", TRIGGER_IIOP_LISTENERS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_IIOP_LISTENER);
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:IiopNameTextProp:IiopNameText", iiopName);
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr", networkAddress);
        setFieldValue("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort", listenerPort);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "a");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_IIOP_LISTENERS);
        assertTrue(isTextPresent(iiopName));

        clickAndWait(getLinkIdByLinkText("propertyForm:configs", iiopName), TRIGGER_EDIT_IIOP_LISTENER);
        assertEquals(networkAddress, getFieldValue("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr"));
        assertEquals(listenerPort, getFieldValue("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort"));

        assertTableRowCount("propertyForm:basicTable", count);

        // access the SSL Page
        clickAndWait("propertyForm:iiopTab:sslEdit", TRIGGER_EDIT_IIOP_SSL);
        setFieldValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname", certName);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertEquals(certName, getFieldValue("propertyForm:propertySheet:propertySheetSection:CertNicknameProp:CertNickname"));

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_IIOP_LISTENERS);

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", iiopName);
    }

    @Test
    public void testORB() {
        final String totalConn = "1048";
        final String maxMsgSize = "2048";
        clickAndWait("treeForm:tree:configurations:server-config:orb:orb_link", TRIGGER_ORB);
        setFieldValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns", totalConn);
        selectDropdownOption("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize", maxMsgSize);
        clickAndWait("form1:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        assertEquals(totalConn, getFieldValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns"));
        assertEquals(maxMsgSize, getFieldValue("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize"));

	// Load default button functionality is broken in all pages, once fixed need to uncomment
        //clickAndWaitForButtonEnabled("form1:propertyContentPage:loadDefaultsButton");
        //assertEquals("1024", getFieldValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns"));
        //assertEquals("1024", getFieldValue("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize"));

    }
}
