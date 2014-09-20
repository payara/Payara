/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class EnterpriseServerTest extends BaseSeleniumTestClass {
    public static final String TRIGGER_ADVANCED_APPLICATIONS_CONFIGURATION = "i18nc.domain.AppsConfigPageHelp";
    public static final String TRIGGER_GENERAL_INFORMATION = "i18n.instance.GeneralTitle";
    public static final String TRIGGER_ADVANCED_DOMAIN_ATTRIBUTES = "i18nc.domain.DomainAttrsPageTitleHelp";
    public static final String TRIGGER_SYSTEM_PROPERTIES = "i18n.common.AdditionalProperties"; // There is no page help on sysprops pages anymore, it seems
    public static final String TRIGGER_RESOURCES = "i18nc.resourcesTarget.pageTitleHelp";


    @Test
    public void testAdvancedDomainAttributes() {
        gotoDasPage();
        clickByIdAction("treeForm:tree:nodes:nodes_link");
        clearByIdAction("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale");
        sendKeysByIdAction("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "en");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
        assertEquals("New values successfully saved.", driver.findElement(By.cssSelector("span.label_sun4")).getText());
        clickByIdAction("treeForm:tree:nodes:nodes_link");
        try {
            assertEquals("en", getValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "value"));
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
    }

    @Test
    public void testSystemProperties() {
        final String property = generateRandomString();
        final String value = property + "value";

        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:serverInstProps");

        int count = addTableRow("propertyForm:sysPropsTable", "propertyForm:sysPropsTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:col2:col1St", property);
        sleep(500);
        setFieldValue("propertyForm:sysPropsTable:rowGroup1:0:overrideValCol:overrideVal", value);
        clickAndWait("propertyForm:SysPropsPage:topButtons:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:serverInstProps");
        assertTableRowCount("propertyForm:sysPropsTable", count);
        
        //delete the property used to test
        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:serverInstProps");
        String prefix = getTableRowByVal("propertyForm:sysPropsTable", property, "col2:col1St");
        String selectId = prefix + "col1:select";
        clickByIdAction(selectId);
        clickByIdAction("propertyForm:sysPropsTable:topActionsGroup1:button1");
        waitforBtnDisable("propertyForm:sysPropsTable:topActionsGroup1:button1");
        clickAndWait("propertyForm:SysPropsPage:topButtons:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    @Test
    public void testServerResourcesPage() {
        final String jndiName = "jdbcResource"+generateRandomString();
        final String description = "devtest test for server->resources page- " + jndiName;
        final String tableID = "propertyForm:resourcesTable";

        gotoDasPage();
        JdbcTest jdbcTest = new JdbcTest();
        jdbcTest.createJDBCResource(jndiName, description, "server", "server");
        
        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:resources");
        String prefix = getTableRowByValue(tableID, jndiName, "col1");
        assertTrue(isTextPresent(prefix, jndiName, tableID));

        int jdbcCount = getTableRowCountByValue(tableID, "JDBC Resources", "col3:type");
        int customCount = getTableRowCountByValue(tableID, "Custom Resources", "col3:type");

        Select select = new Select(driver.findElement(By.id("propertyForm:resourcesTable:topActionsGroup1:filter_list")));
        select.selectByVisibleText("Custom Resources");
        waitForTableRowCount(tableID, customCount);

        select = new Select(driver.findElement(By.id("propertyForm:resourcesTable:topActionsGroup1:filter_list")));
        select.selectByVisibleText("JDBC Resources");
        waitForTableRowCount(tableID, jdbcCount);

        String clickId = getTableRowByValue(tableID, jndiName, "col1") + "col1:link";
        clickByIdAction(clickId);
        waitForButtonEnabled("propertyForm:propertyContentPage:topButtons:saveButton");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");

        jdbcTest.deleteJDBCResource(jndiName, "server", "server");
    }

    public void waitForTableRowCount(String tableID, int count) {
        for (int i = 0;; i++) {
            if (i >= 1000) {
                Assert.fail("timeout");
            }
            try {
                int tableCount = getTableRowCount(tableID);
                if (tableCount == count) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sleep(500);
        }
    }

    public void gotoDasPage() {
        driver.get(baseUrl + "/common/index.jsf");
        clickByIdAction("treeForm:tree:applicationServer:applicationServer_link");
    }
}
