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

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: jasonlee
 * Date: Mar 22, 2010
 * Time: 4:31:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_APPLICATIONS = "Applications can be enterprise or web applications, or various kinds of modules.";
    private static final String TRIGGER_APPLICATIONS_DEPLOY = "Packaged File to Be Uploaded to the Server";
    private static final String TRIGGER_APPLICATION_DISABLED = "Selected application(s) has been disabled.";
    private static final String TRIGGER_EDIT_APPLICATION = "Edit Application";
    private static final String TRIGGER_APPLICATION_ENABLED = "Selected application(s) has been enabled.";

    private static final String ELEMENT_STATUS = "propertyForm:propertySheet:propertSectionTextField:statusProp:status";
    private static final String ELEMENT_APP_NAME = "form:war:psection:nameProp:appName";
    private static final String ELEMENT_CONTEXT_ROOT = "form:war:psection:cxp:ctx";
    private static final String ELEMENT_UNDEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:button1";
    private static final String ELEMENT_DEPLOY_TABLE = "propertyForm:deployTable";
    private static final String ELEMENT_ENABLE_BUTTON = "propertyForm:deployTable:topActionsGroup1:button2";
    private static final String ELEMENT_CANCEL_BUTTON = "propertyForm:propertyContentPage:topButtons:cancelButton";
    private static final String ELEMENT_DISABLE_BUTTON = "propertyForm:deployTable:topActionsGroup1:button3";
    private static final String ELEMENT_UPLOAD_BUTTON = "form:title:topButtons:uploadButton";
    private static final String ELEMENT_FILE_FIELD = "form:sheet1:section1:prop1:fileupload";
    private static final String ELEMENT_DEPLOY_BUTTON = "propertyForm:deployTable:topActionsGroup1:deployButton";
    private static final String TRIGGER_SUCCESS = "New values successfully saved";
    private static final String TRIGGER_DOMAIN_ATTRIBUTES = "Domain Attributes";
    private static final String TRIGGER_DOMAIN_LOGS = "i18nc.domainLogs.PageHelp";
    private static final String TRIGGER_APPLICATION_CONFIGURATION = "Enable reloading so that changes to deployed applications";

    //The following test will pass ONLY if there is no cluster or standalone instance.  This is for "PE" profile
    //TODO: We may need to DELETE all cluster and standalone instance in the beginning of this test.

    //@Test
    public void testDeployWar() {
        final String applicationName = generateRandomString();
        clickAndWait("treeForm:tree:applications:applications_link", TRIGGER_APPLICATIONS);
        int preCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);

        // hrm
        clickAndWaitForElement(ELEMENT_DEPLOY_BUTTON, ELEMENT_FILE_FIELD);
        File war = new File("src/test/resources/test.war");
        try {
            selectFile(ELEMENT_FILE_FIELD, war.toURL().toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        assertEquals("test", getFieldValue(ELEMENT_CONTEXT_ROOT));
        assertEquals("test", getFieldValue(ELEMENT_APP_NAME));

        setFieldValue(ELEMENT_CONTEXT_ROOT, applicationName);
        setFieldValue(ELEMENT_APP_NAME, applicationName);

        clickAndWait(ELEMENT_UPLOAD_BUTTON, TRIGGER_APPLICATIONS);
        String conf = "";
        if (isAlertPresent()) {
            conf = getAlertText();
        }
        int postCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount < postCount);

        clickAndWait("propertyForm:deployTable:rowGroup1:0:col4:link", applicationName);
        
        // Disable application
        selectTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName);
        clickAndWait(ELEMENT_DISABLE_BUTTON, TRIGGER_APPLICATION_DISABLED);
        clickAndWait(getLinkIdByLinkText(ELEMENT_DEPLOY_TABLE, applicationName), TRIGGER_EDIT_APPLICATION);
        assertEquals("off", getFieldValue(ELEMENT_STATUS));
        clickAndWait(ELEMENT_CANCEL_BUTTON, TRIGGER_APPLICATIONS);

//        testDisableButton(applicationName, ELEMENT_DEPLOY_TABLE, ELEMENT_DISABLE_BUTTON, ELEMENT_STATUS, ELEMENT_CANCEL_BUTTON, TRIGGER_APPLICATIONS, TRIGGER_EDIT_APPLICATION);


        // Enable Application
        selectTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName);
        clickAndWait(ELEMENT_ENABLE_BUTTON, TRIGGER_APPLICATION_ENABLED, 300);
        clickAndWait(getLinkIdByLinkText(ELEMENT_DEPLOY_TABLE, applicationName), TRIGGER_EDIT_APPLICATION);
        assertEquals("on", getFieldValue(ELEMENT_STATUS));
        clickAndWait(ELEMENT_CANCEL_BUTTON, TRIGGER_APPLICATIONS);


//        testEnableButton(applicationName, ELEMENT_DEPLOY_TABLE, ELEMENT_ENABLE_BUTTON, ELEMENT_STATUS, ELEMENT_CANCEL_BUTTON, TRIGGER_APPLICATIONS, TRIGGER_EDIT_APPLICATION);

        // Undeploy application
        chooseOkOnNextConfirmation();
        selectTableRowByValue(ELEMENT_DEPLOY_TABLE, applicationName);
        pressButton(ELEMENT_UNDEPLOY_BUTTON);
        getConfirmation();
        waitForPageLoad(applicationName, true);
        int postUndeployCount = this.getTableRowCount(ELEMENT_DEPLOY_TABLE);
        assertTrue (preCount == postUndeployCount);
    }

    @Test
    public void testApplicationConfiguration() {
        final String adminTimeout = Integer.toString(generateRandomNumber(100));
        clickAndWait("treeForm:tree:nodes:nodes_link", TRIGGER_DOMAIN_ATTRIBUTES);
        clickAndWait("propertyForm:domainTabs:appConfig", TRIGGER_APPLICATION_CONFIGURATION);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout", adminTimeout);
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_SUCCESS);
        assertEquals(adminTimeout, getFieldValue("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout"));
    }

    @Test
    public void testDomainAttributes() {
        clickAndWait("treeForm:tree:nodes:nodes_link", TRIGGER_DOMAIN_ATTRIBUTES);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "en_UK");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_SUCCESS);
        assertEquals("en_UK", getFieldValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale"));
    }

    @Test
    public void testDomainLogs() {
        clickAndWait("treeForm:tree:nodes:nodes_link", TRIGGER_DOMAIN_ATTRIBUTES);
        clickAndWait("propertyForm:domainTabs:domainLogs", resolveTriggerText(TRIGGER_DOMAIN_LOGS));
        // click download, but ignore it (selenium can't interect with Save File dialog
        pressButton("form:propertyContentPage:topButtons:collectLogFiles");
        // if above is broken, assertion will fail
        assertTrue(isTextPresent(TRIGGER_DOMAIN_LOGS));
    }
}
