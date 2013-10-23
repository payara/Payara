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
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.*;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class ResourceAdapterConfigsTest extends BaseSeleniumTestClass {
    @Test
    public void testResourceAdapterConfigs() throws Exception {
            gotoDasPage();
            clickAndWait("treeForm:tree:resources:resourceAdapterConfigs:resourceAdapterConfigs_link");
            int emptyCount = getTableRowCountByValue("propertyForm:poolTable", "jmsra", "col1:link", true);
            if (emptyCount != 0){
                gotoDasPage();
                clickAndWait("treeForm:tree:resources:resourceAdapterConfigs:resourceAdapterConfigs_link");
                deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", "jmsra");
            }

            // Create new RA config
            clickAndWait("propertyForm:poolTable:topActionsGroup1:newButton");
            Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:threadPoolsIdProp:threadpoolsid")));
            select.selectByVisibleText("thread-pool-1");
            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");

            // Verify config was saved and update values
            String prefix = getTableRowByValue("propertyForm:poolTable", "jmsra", "col1");
            assertEquals("jmsra", getText(prefix + "col1:link"));
            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);
            Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:threadPoolsIdProp:threadpoolsid")));
            assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("thread-pool-1"));
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
            
            // Remove config
            gotoDasPage();
            clickAndWait("treeForm:tree:resources:resourceAdapterConfigs:resourceAdapterConfigs_link");
            deleteRow("propertyForm:poolTable:topActionsGroup1:button1", "propertyForm:poolTable", "jmsra");
    }
}
