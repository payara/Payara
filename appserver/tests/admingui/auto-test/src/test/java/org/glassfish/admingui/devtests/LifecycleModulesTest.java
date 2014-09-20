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

import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 * @author jeremylv
 *
 */
public class LifecycleModulesTest extends BaseSeleniumTestClass {

    public static final String ID_LIFECYCLE_TABLE = "propertyForm:deployTable";
    
    @Test
    public void testLifecycleModules() {
        final String lifecycleName = "TestLifecycle"+generateRandomString();
        final String lifecycleClassname = "org.foo.nonexistent.Lifecyclemodule";

        gotoDasPage();
        clickByIdAction("treeForm:tree:lifecycles:lifecycles_link");
        clickByIdAction("propertyForm:deployTable:topActionsGroup1:newButton");
        setFieldValue("form:propertySheet:propertSectionTextField:IdTextProp:IdText", lifecycleName);
        setFieldValue("form:propertySheet:propertSectionTextField:classNameProp:classname", lifecycleClassname);
        clickByIdAction("form:propertyContentPage:topButtons:newButton");
        String prefix = getTableRowByValue(ID_LIFECYCLE_TABLE, lifecycleName, "col1");
        try {
            assertEquals(lifecycleName, getText(prefix + "col1:link"));
        } catch (Error e) {
            verificationErrors.append(e.toString());
        };
        
        //test Disable button and add some property 
        String clickId = getTableRowByValue(ID_LIFECYCLE_TABLE, lifecycleName, "col1")+"col0:select";
        testDisableButton(clickId, prefix);
        
        //test Enable button and delete some property
        testEnableButton(clickId, prefix);
        
        //delete the lifecycle
        testDeleteButton(clickId);
    }

    private void testDeleteButton(String clickId) {
        gotoDasPage();
        clickByIdAction("treeForm:tree:lifecycles:lifecycles_link");
        clickByIdAction(clickId);
        clickByIdAction("propertyForm:deployTable:topActionsGroup1:button1");
        String msg = closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        try {
            assertTrue(msg.indexOf("Selected Lifecycle Module(s) will be deleted.") != -1);
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
    }

    private void testEnableButton(String clickId, String prefix) {
        gotoDasPage();
        clickByIdAction("treeForm:tree:lifecycles:lifecycles_link");
        clickByIdAction(clickId);
        clickByIdAction("propertyForm:deployTable:topActionsGroup1:button2");
        isCheckboxSelected(clickId);
        clickByIdAction(prefix + "col1:link");
        
        //delete property
        isElementPresent("propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        clickByIdAction("propertyForm:basicTable:rowGroup1:0:col1:select");
        clickByIdAction("propertyForm:basicTable:topActionsGroup1:button1");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
        assertEquals(true, driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:statusEdit:status")).isSelected());
    }
    
    private void testDisableButton(String clickId, String prefix) {
        clickByIdAction(clickId);
        isElementPresent("propertyForm:deployTable:topActionsGroup1:button3");
        clickByIdAction("propertyForm:deployTable:topActionsGroup1:button3");
        isCheckboxSelected(clickId);
        clickByIdAction(prefix + "col1:link");
        
        //add property and verify
        isElementPresent("propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        int lifecyclePropCount = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St","test");
        sleep(500);
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St","value");
        clickByIdAction("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        assertTableRowCount("propertyForm:basicTable", lifecyclePropCount);
        assertEquals(false, driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:statusEdit:status")).isSelected());
    }
}
